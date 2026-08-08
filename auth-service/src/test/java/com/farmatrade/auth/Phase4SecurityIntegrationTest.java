package com.farmatrade.auth;

import com.farmatrade.auth.dto.RegisterRequest;
import com.farmatrade.auth.entity.Role;
import com.farmatrade.auth.repository.SecurityAuditEventRepository;
import com.farmatrade.auth.repository.UserRepository;
import com.farmatrade.auth.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "auth.login.max-failed-attempts=2",
        "auth.login.lock-minutes=15",
        "auth.cors.allowed-origins=http://localhost:5173"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase4SecurityIntegrationTest {
    private static final String PASSWORD = "StrongPass1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityAuditEventRepository auditRepository;

    @Autowired
    private RegistrationService registrationService;

    @BeforeEach
    void clean() {
        auditRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void authenticatedUserCanViewOnlySafeOwnProfile() throws Exception {
        seed("Farmer", "farmer@example.com", "9000000301", "900000000002", Role.FARMER);
        String token = loginAndToken("farmer", "farmer@example.com", PASSWORD);

        String body = mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("farmer@example.com"))
                .andExpect(jsonPath("$.role").value("FARMER"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.createdByAdminId").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.aadhaar").doesNotExist())
                .andExpect(jsonPath("$.aadhaarHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(PASSWORD, "900000000002", "aadhaarHash", "passwordHash");
    }

    @Test
    void farmerAndBuyerCannotUseAdminApis() throws Exception {
        seed("Farmer", "farmer@example.com", "9000000311", "900000000002", Role.FARMER);
        seed("Buyer", "buyer@example.com", "9000000312", "911111111118", Role.BUYER);

        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(loginAndToken("farmer", "farmer@example.com", PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mockMvc.perform(get("/api/admin/audit-events").header(HttpHeaders.AUTHORIZATION, bearer(loginAndToken("buyer", "buyer@example.com", PASSWORD))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListSearchFilterAndDisableOrEnableNormalAccounts() throws Exception {
        seed("Admin", "admin@example.com", "9000000321", "900000000002", Role.ADMIN);
        seed("Farmer Alpha", "farmer-alpha@example.com", "9000000322", "911111111118", Role.FARMER);
        seed("Buyer Beta", "buyer-beta@example.com", "9000000323", "922222222224", Role.BUYER);
        String adminToken = loginAndToken("admin", "admin@example.com", PASSWORD);
        Long farmerId = userRepository.findByEmailIgnoreCase("farmer-alpha@example.com").orElseThrow().getId();

        mockMvc.perform(get("/api/admin/users")
                        .param("search", "alpha")
                        .param("role", "FARMER")
                        .param("enabled", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("farmer-alpha@example.com"))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].aadhaarHash").doesNotExist());

        mockMvc.perform(patch("/api/admin/users/{id}/status", farmerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
        assertThat(userRepository.findById(farmerId).orElseThrow().isEnabled()).isFalse();

        mockMvc.perform(patch("/api/admin/users/{id}/status", farmerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void adminCannotDisableSelfOrLastActiveAdmin() throws Exception {
        seed("Admin", "admin@example.com", "9000000331", "900000000002", Role.ADMIN);
        seed("Admin Two", "admin2@example.com", "9000000332", "911111111118", Role.ADMIN);
        String adminToken = loginAndToken("admin", "admin@example.com", PASSWORD);
        Long adminId = userRepository.findByEmailIgnoreCase("admin@example.com").orElseThrow().getId();
        Long admin2Id = userRepository.findByEmailIgnoreCase("admin2@example.com").orElseThrow().getId();

        mockMvc.perform(patch("/api/admin/users/{id}/status", adminId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/users/{id}/status", admin2Id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/users/{id}/status", adminId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void failedLoginLockoutAndResetBehaviorWorks() throws Exception {
        seed("Farmer", "farmer@example.com", "9000000341", "900000000002", Role.FARMER);

        login("farmer", "farmer@example.com", "WrongPass1!").andExpect(status().isUnauthorized());
        assertThat(userRepository.findByEmailIgnoreCase("farmer@example.com").orElseThrow().getFailedLoginAttempts()).isEqualTo(1);

        loginAndToken("farmer", "farmer@example.com", PASSWORD);
        assertThat(userRepository.findByEmailIgnoreCase("farmer@example.com").orElseThrow().getFailedLoginAttempts()).isZero();

        login("farmer", "farmer@example.com", "WrongPass1!").andExpect(status().isUnauthorized());
        login("farmer", "farmer@example.com", "WrongPass1!").andExpect(status().isUnauthorized());
        var locked = userRepository.findByEmailIgnoreCase("farmer@example.com").orElseThrow();
        assertThat(locked.getLockedUntil()).isNotNull();

        login("farmer", "farmer@example.com", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void disabledAndLockedUsersCannotUseProtectedApis() throws Exception {
        seed("Admin", "admin@example.com", "9000000351", "900000000002", Role.ADMIN);
        seed("Farmer", "farmer@example.com", "9000000352", "911111111118", Role.FARMER);
        String adminToken = loginAndToken("admin", "admin@example.com", PASSWORD);
        String farmerToken = loginAndToken("farmer", "farmer@example.com", PASSWORD);
        Long farmerId = userRepository.findByEmailIgnoreCase("farmer@example.com").orElseThrow().getId();

        mockMvc.perform(patch("/api/admin/users/{id}/status", farmerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(farmerToken)))
                .andExpect(status().isUnauthorized());
        login("farmer", "farmer@example.com", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void auditEventsAreAdminVisibleAndContainNoSensitiveValues() throws Exception {
        seed("Admin", "admin@example.com", "9000000361", "900000000002", Role.ADMIN);
        seed("Farmer", "farmer@example.com", "9000000362", "911111111118", Role.FARMER);
        String adminToken = loginAndToken("admin", "admin@example.com", PASSWORD);
        login("farmer", "farmer@example.com", "WrongPass1!").andExpect(status().isUnauthorized());

        String body = mockMvc.perform(get("/api/admin/audit-events").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("REGISTRATION", "SUCCESSFUL_LOGIN", "FAILED_LOGIN");
        assertThat(body).doesNotContain(PASSWORD, "WrongPass1!", "900000000002", "911111111118", "aadhaar", "passwordHash");
    }

    @Test
    void corsPreflightAndStatelessSecurityAreConfigured() throws Exception {
        mockMvc.perform(options("/api/admin/users")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_authentication"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    private void seed(String name, String email, String mobile, String aadhaar, Role role) {
        registrationService.register(new RegisterRequest(name, email, mobile, PASSWORD, aadhaar), role, null);
    }

    private org.springframework.test.web.servlet.ResultActions login(String portal, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login/" + portal)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"identifier":"%s","password":"%s"}
                        """.formatted(email, password)));
    }

    private String loginAndToken(String portal, String email, String password) throws Exception {
        String body = login(portal, email, password)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
