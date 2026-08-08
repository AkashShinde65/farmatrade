package com.farmatrade.auth.service;

import com.farmatrade.auth.dto.RegisterRequest;
import com.farmatrade.auth.dto.UserRegistrationResponse;
import com.farmatrade.auth.entity.Role;
import com.farmatrade.auth.entity.User;
import com.farmatrade.auth.exception.DuplicateResourceException;
import com.farmatrade.auth.repository.UserRepository;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private static final Pattern STRONG_PASSWORD =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{10,72}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AadhaarValidationService aadhaarValidationService;
    private final AuditService auditService;

    public RegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AadhaarValidationService aadhaarValidationService,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.aadhaarValidationService = aadhaarValidationService;
        this.auditService = auditService;
    }

    @Transactional
    public UserRegistrationResponse register(RegisterRequest request, Role role, Long createdByAdminId) {
        validatePasswordStrength(request.password());
        String aadhaar = aadhaarValidationService.normalize(request.aadhaar());
        rejectDuplicates(request, aadhaar);

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setMobile(request.mobile().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAadhaarHash(aadhaar);
        user.setRole(role);
        user.setCreatedByAdminId(createdByAdminId);
        user.setEnabled(true);
        User saved = userRepository.save(user);
        auditService.record(
                role == Role.ADMIN ? "ADMIN_CREATED" : "REGISTRATION",
                createdByAdminId,
                saved.getId(),
                role == Role.ADMIN ? "Admin account created" : role.name() + " account registered"
        );
        return UserRegistrationResponse.from(saved);
    }

    private void rejectDuplicates(RegisterRequest request, String aadhaarHash) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Duplicate identity");
        }
        if (userRepository.existsByMobile(request.mobile())) {
            throw new DuplicateResourceException("Duplicate identity");
        }
        if (userRepository.existsByAadhaarHash(aadhaarHash)) {
            throw new DuplicateResourceException("Duplicate identity");
        }
    }

    private void validatePasswordStrength(String password) {
        if (password == null || !STRONG_PASSWORD.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must be 10-72 characters and include uppercase, lowercase, number and symbol");
        }
    }
}
