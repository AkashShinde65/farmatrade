package com.farmatrade.auth;

import com.farmatrade.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationApiIntegrationTest {
    private static final String PASSWORD = "StrongPass1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    @Test
    void farmerRegistrationAlwaysAssignsFarmerAndStoresProtectedIdentity() throws Exception {
        mockMvc.perform(post("/api/auth/register/farmer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Farmer One", "farmer@example.com", "9000000001", PASSWORD, "900000000002", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("FARMER"))
                .andExpect(jsonPath("$.aadhaar").doesNotExist())
                .andExpect(jsonPath("$.aadhaarHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        var user = userRepository.findByEmailIgnoreCase("farmer@example.com").orElseThrow();
        assertThat(user.getAadhaarHash()).hasSize(64).doesNotContain("900000000002");
        assertThat(user.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isTrue();
    }

    @Test
    void buyerRegistrationAlwaysAssignsBuyer() throws Exception {
        mockMvc.perform(post("/api/auth/register/buyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Buyer One", "buyer@example.com", "9000000002", PASSWORD, "911111111118", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("BUYER"));
    }

    @Test
    void clientCannotSelectRoleInRequestBody() throws Exception {
        mockMvc.perform(post("/api/auth/register/farmer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Role Try", "roletry@example.com", "9000000003", PASSWORD, "922222222224", ",\"role\":\"ADMIN\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("FARMER"));

        assertThat(userRepository.findByEmailIgnoreCase("roletry@example.com").orElseThrow().getRole().name())
                .isEqualTo("FARMER");
    }

    @Test
    void invalidChecksumReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register/farmer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Bad Aadhaar", "bad@example.com", "9000000004", PASSWORD, "900000000003", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
    }

    @Test
    void duplicateEmailMobileAndAadhaarReturnConflict() throws Exception {
        mockMvc.perform(post("/api/auth/register/farmer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Original", "original@example.com", "9000000005", PASSWORD, "933333333334", "")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register/buyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Duplicate Email", "original@example.com", "9000000006", PASSWORD, "944444444442", "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("duplicate_account"));

        mockMvc.perform(post("/api/auth/register/buyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Duplicate Mobile", "mobile@example.com", "9000000005", PASSWORD, "944444444442", "")))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/register/buyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Duplicate Aadhaar", "aadhaar@example.com", "9000000007", PASSWORD, "933333333334", "")))
                .andExpect(status().isConflict());
    }

    @Test
    void weakPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register/farmer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Weak", "weak@example.com", "9000000008", "password", "955555555557", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminRegistrationEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Admin", "admin@example.com", "9000000009", PASSWORD, "955555555557", "")))
                .andExpect(status().isUnauthorized());
    }

    private String registerJson(String fullName, String email, String mobile, String password, String aadhaar, String extraFields) {
        return """
                {"fullName":"%s","email":"%s","mobile":"%s","password":"%s","aadhaar":"%s"%s}
                """.formatted(fullName, email, mobile, password, aadhaar, extraFields);
    }
}
