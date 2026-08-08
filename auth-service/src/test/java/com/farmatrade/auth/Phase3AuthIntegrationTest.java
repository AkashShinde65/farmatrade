package com.farmatrade.auth;

import com.farmatrade.auth.dto.RegisterRequest;
import com.farmatrade.auth.entity.Role;
import com.farmatrade.auth.repository.UserRepository;
import com.farmatrade.auth.security.JwtUtil;
import com.farmatrade.auth.service.RegistrationService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase3AuthIntegrationTest {
    private static final String PASSWORD = "StrongPass1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    @Test
    void roleSpecificLoginsReturnGenericUnauthorizedForWrongPortals() throws Exception {
        seed("Farmer", "farmer@example.com", "9000000201", "900000000002", Role.FARMER);
        seed("Buyer", "buyer@example.com", "9000000202", "911111111118", Role.BUYER);
        seed("Admin", "admin@example.com", "9000000203", "922222222224", Role.ADMIN);

        login("farmer", "farmer@example.com").andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("FARMER"));
        login("buyer", "buyer@example.com").andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("BUYER"));
        login("admin", "admin@example.com").andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("ADMIN"));

        login("buyer", "farmer@example.com").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid credentials"));
        login("admin", "farmer@example.com").andExpect(status().isUnauthorized());
        login("farmer", "buyer@example.com").andExpect(status().isUnauthorized());
        login("admin", "buyer@example.com").andExpect(status().isUnauthorized());
        login("farmer", "missing@example.com").andExpect(status().isUnauthorized());
    }

    @Test
    void disabledAccountCannotLogIn() throws Exception {
        seed("Farmer", "farmer@example.com", "9000000211", "900000000002", Role.FARMER);
        var user = userRepository.findByEmailIgnoreCase("farmer@example.com").orElseThrow();
        user.setEnabled(false);
        userRepository.saveAndFlush(user);

        login("farmer", "farmer@example.com").andExpect(status().isUnauthorized());
    }

    @Test
    void jwtHasExpectedClaimsAndJwksVerifiesItWithoutSensitiveData() throws Exception {
        seed("Farmer", "farmer@example.com", "9000000221", "900000000002", Role.FARMER);
        String token = loginAndToken("farmer", "farmer@example.com");
        SignedJWT jwt = SignedJWT.parse(token);

        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(jwt.getHeader().getKeyID()).isEqualTo("farmatrade-auth-rs256-1");
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("farmatrade-auth-service");
        assertThat(jwt.getJWTClaimsSet().getAudience()).contains("farmatrade-api");
        assertThat(jwt.getJWTClaimsSet().getStringClaim("role")).isEqualTo("FARMER");
        assertThat(jwt.getJWTClaimsSet().getStringClaim("email")).isEqualTo("farmer@example.com");
        assertThat(jwt.getJWTClaimsSet().getSubject()).isNotBlank();
        assertThat(jwt.getJWTClaimsSet().getJWTID()).isNotBlank();
        assertThat(jwt.getJWTClaimsSet().getExpirationTime()).isAfter(Date.from(Instant.now().plusSeconds(14 * 60)));
        assertThat(jwt.getJWTClaimsSet().getClaims()).doesNotContainKeys("password", "passwordHash", "aadhaar", "aadhaarHash");

        String jwksBody = mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kid").value("farmatrade-auth-rs256-1"))
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].n").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].e").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].d").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        RSAKey publicKey = (RSAKey) JWKSet.parse(jwksBody).getKeyByKeyId("farmatrade-auth-rs256-1");
        assertThat(jwt.verify(new RSASSAVerifier(publicKey.toRSAPublicKey()))).isTrue();
    }

    @Test
    void invalidExpiredWrongIssuerAndWrongAudienceTokensAreRejected() throws Exception {
        seed("Farmer", "farmer@example.com", "9000000231", "900000000002", Role.FARMER);
        String token = loginAndToken("farmer", "farmer@example.com");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("farmer@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.aadhaarHash").doesNotExist());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token.substring(0, token.length() - 2) + "xx"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        assertThatThrownBy(() -> jwtUtil.validate(tokenWith("bad-issuer", "farmatrade-api", -1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwtUtil.validate(tokenWith("farmatrade-auth-service", "bad-audience", -1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwtUtil.validate(tokenWith("farmatrade-auth-service", "farmatrade-api", 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adminRegistrationRequiresAdminTokenAndRecordsCreator() throws Exception {
        seed("Farmer", "farmer@example.com", "9000000241", "900000000002", Role.FARMER);
        seed("Buyer", "buyer@example.com", "9000000242", "911111111118", Role.BUYER);
        seed("Admin", "admin@example.com", "9000000243", "922222222224", Role.ADMIN);

        String farmerToken = loginAndToken("farmer", "farmer@example.com");
        String buyerToken = loginAndToken("buyer", "buyer@example.com");
        String adminToken = loginAndToken("admin", "admin@example.com");
        Long creatorId = userRepository.findByEmailIgnoreCase("admin@example.com").orElseThrow().getId();

        adminRegister("farmer-created@example.com", "9000000244", "933333333334", farmerToken)
                .andExpect(status().isForbidden());
        adminRegister("buyer-created@example.com", "9000000245", "944444444442", buyerToken)
                .andExpect(status().isForbidden());

        adminRegister("admin2@example.com", "9000000246", "955555555557", adminToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.createdByAdminId").value(creatorId))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.aadhaarHash").doesNotExist());

        var created = userRepository.findByEmailIgnoreCase("admin2@example.com").orElseThrow();
        assertThat(created.getRole()).isEqualTo(Role.ADMIN);
        assertThat(created.getCreatedByAdminId()).isEqualTo(creatorId);
        assertThat(passwordEncoder.matches(PASSWORD, created.getPasswordHash())).isTrue();
    }

    private org.springframework.test.web.servlet.ResultActions login(String portal, String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login/" + portal)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"identifier":"%s","password":"%s"}
                        """.formatted(email, PASSWORD)));
    }

    private String loginAndToken(String portal, String email) throws Exception {
        String body = login(portal, email)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }

    private org.springframework.test.web.servlet.ResultActions adminRegister(
            String email,
            String mobile,
            String aadhaar,
            String token
    ) throws Exception {
        return mockMvc.perform(post("/api/auth/register/admin")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Admin Two", email, mobile, aadhaar)));
    }

    private void seed(String name, String email, String mobile, String aadhaar, Role role) {
        registrationService.register(new RegisterRequest(name, email, mobile, PASSWORD, aadhaar), role, null);
    }

    private String registerJson(String fullName, String email, String mobile, String aadhaar) {
        return """
                {"fullName":"%s","email":"%s","mobile":"%s","password":"%s","aadhaar":"%s"}
                """.formatted(fullName, email, mobile, PASSWORD, aadhaar);
    }

    private String tokenWith(String issuer, String audience, long expiredMinutes) {
        var user = userRepository.findByEmailIgnoreCase("farmer@example.com").orElseThrow();
        String originalIssuer = System.getProperty("unused", issuer);
        return TestJwtFactory.createTokenLike(jwtUtil, user, issuer, audience, expiredMinutes);
    }
}
