package com.farmatrade.auth;

import com.farmatrade.auth.entity.Role;
import com.farmatrade.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:farmatrade_auth_bootstrap_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "initial-admin.name=Bootstrap Admin",
        "initial-admin.email=bootstrap-admin@example.com",
        "initial-admin.mobile=9000000100",
        "initial-admin.password=StrongPass1!",
        "initial-admin.aadhaar=955555555557"
})
@ActiveProfiles("test")
class InitialAdminBootstrapTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void createsInitialAdminFromEnvironmentLikePropertiesOnlyWhenNoAdminExists() {
        var admin = userRepository.findByEmailIgnoreCase("bootstrap-admin@example.com").orElseThrow();

        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getPasswordHash()).startsWith("$2");
        assertThat(admin.getAadhaarHash()).hasSize(64).doesNotContain("955555555557");
        assertThat(userRepository.existsByRole(Role.ADMIN)).isTrue();
    }
}
