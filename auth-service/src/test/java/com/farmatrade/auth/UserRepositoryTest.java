package com.farmatrade.auth;

import com.farmatrade.auth.entity.Role;
import com.farmatrade.auth.entity.User;
import com.farmatrade.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void checksUniqueEmailMobileAndAadhaarHash() {
        User user = new User();
        user.setFullName("Phase One User");
        user.setEmail("phase1@example.com");
        user.setMobile("9000000001");
        user.setPasswordHash("$2a$12$placeholderplaceholderplaceholderplaceholderplaceholder123456");
        user.setAadhaarHash("aadhaar-hmac-sha256-placeholder");
        user.setRole(Role.FARMER);
        user.setEnabled(true);

        userRepository.saveAndFlush(user);

        assertThat(userRepository.existsByEmailIgnoreCase("PHASE1@example.com")).isTrue();
        assertThat(userRepository.existsByMobile("9000000001")).isTrue();
        assertThat(userRepository.existsByAadhaarHash("aadhaar-hmac-sha256-placeholder")).isTrue();
        assertThat(userRepository.findByEmailIgnoreCase("phase1@example.com")).isPresent();
        assertThat(userRepository.findByEmailIgnoreCaseOrMobile("no-match", "9000000001")).isPresent();
        assertThat(userRepository.existsByRole(Role.FARMER)).isTrue();
    }
}
