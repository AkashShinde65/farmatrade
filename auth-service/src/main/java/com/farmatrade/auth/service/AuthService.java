package com.farmatrade.auth.service;

import com.farmatrade.auth.dto.JwtResponse;
import com.farmatrade.auth.dto.LoginRequest;
import com.farmatrade.auth.dto.UserProfileResponse;
import com.farmatrade.auth.dto.UserRegistrationResponse;
import com.farmatrade.auth.entity.Role;
import com.farmatrade.auth.entity.User;
import com.farmatrade.auth.exception.InvalidCredentialsException;
import com.farmatrade.auth.repository.UserRepository;
import com.farmatrade.auth.security.JwtUtil;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;
    private final Clock clock;
    private final int failedLoginLimit;
    private final Duration lockDuration;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuditService auditService,
            Clock clock,
            @Value("${auth.login.max-failed-attempts:5}") int failedLoginLimit,
            @Value("${auth.login.lock-minutes:15}") long lockMinutes
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
        this.clock = clock;
        this.failedLoginLimit = failedLoginLimit;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public JwtResponse login(LoginRequest request, Role requiredRole) {
        String identifier = request.identifier().trim();
        User user = userRepository.findByEmailIgnoreCaseOrMobile(identifier, identifier).orElse(null);
        if (user == null) {
            auditService.record("FAILED_LOGIN", null, null, "Failed login attempt");
            throw new InvalidCredentialsException();
        }
        if (!user.isEnabled() || isLocked(user) || user.getRole() != requiredRole
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new InvalidCredentialsException();
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        auditService.record("SUCCESSFUL_LOGIN", user.getId(), user.getId(), "Successful login");
        return new JwtResponse(jwtUtil.generateToken(user), jwtUtil.expiresInSeconds(), UserRegistrationResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse currentUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isEnabled)
                .filter(user -> !isLocked(user))
                .map(UserProfileResponse::from)
                .orElseThrow(InvalidCredentialsException::new);
    }

    private void handleFailedLogin(User user) {
        if (!user.isEnabled()) {
            auditService.record("FAILED_LOGIN", null, user.getId(), "Failed login attempt");
            return;
        }
        if (isLocked(user)) {
            auditService.record("FAILED_LOGIN", null, user.getId(), "Failed login attempt");
            return;
        }
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        if (user.getFailedLoginAttempts() >= failedLoginLimit) {
            user.setLockedUntil(clock.instant().plus(lockDuration));
            auditService.record("ACCOUNT_LOCKED", null, user.getId(), "Account temporarily locked after failed logins");
        } else {
            auditService.record("FAILED_LOGIN", null, user.getId(), "Failed login attempt");
        }
        userRepository.save(user);
    }

    private boolean isLocked(User user) {
        Instant lockedUntil = user.getLockedUntil();
        return lockedUntil != null && lockedUntil.isAfter(clock.instant());
    }
}
