package com.farmatrade.auth.service;

import com.farmatrade.auth.dto.RegisterRequest;
import com.farmatrade.auth.entity.Role;
import com.farmatrade.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InitialAdminBootstrap implements CommandLineRunner {
    private final String name;
    private final String email;
    private final String mobile;
    private final String password;
    private final String aadhaar;
    private final UserRepository userRepository;
    private final RegistrationService registrationService;

    public InitialAdminBootstrap(
            @Value("${initial-admin.name:}") String name,
            @Value("${initial-admin.email:}") String email,
            @Value("${initial-admin.mobile:}") String mobile,
            @Value("${initial-admin.password:}") String password,
            @Value("${initial-admin.aadhaar:}") String aadhaar,
            UserRepository userRepository,
            RegistrationService registrationService
    ) {
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.password = password;
        this.aadhaar = aadhaar;
        this.userRepository = userRepository;
        this.registrationService = registrationService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        boolean anyConfigured = StringUtils.hasText(name) || StringUtils.hasText(email)
                || StringUtils.hasText(mobile) || StringUtils.hasText(password) || StringUtils.hasText(aadhaar);
        if (!anyConfigured) {
            return;
        }
        if (!StringUtils.hasText(name) || !StringUtils.hasText(email) || !StringUtils.hasText(mobile)
                || !StringUtils.hasText(password) || !StringUtils.hasText(aadhaar)) {
            throw new IllegalStateException("All INITIAL_ADMIN_* values are required to bootstrap the first Admin");
        }
        registrationService.register(new RegisterRequest(name, email, mobile, password, aadhaar), Role.ADMIN, null);
    }
}
