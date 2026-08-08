package com.farmatrade.auth.service;

import com.farmatrade.auth.dto.AdminUserResponse;
import com.farmatrade.auth.entity.Role;
import com.farmatrade.auth.entity.User;
import com.farmatrade.auth.exception.ForbiddenOperationException;
import com.farmatrade.auth.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdminUserService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(String search, Role role, Boolean enabled, Pageable pageable) {
        return userRepository.findAll(filter(search, role, enabled), pageable).map(AdminUserResponse::from);
    }

    @Transactional
    public AdminUserResponse setEnabled(Long actingAdminId, Long targetUserId, boolean enabled) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (target.getId().equals(actingAdminId) && !enabled) {
            throw new ForbiddenOperationException("Admins cannot disable their own account");
        }
        if (target.getRole() == Role.ADMIN && target.isEnabled() && !enabled
                && userRepository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new ForbiddenOperationException("Last active Admin cannot be disabled");
        }
        target.setEnabled(enabled);
        if (!enabled) {
            target.setLockedUntil(null);
        }
        User saved = userRepository.save(target);
        auditService.record(
                enabled ? "ACCOUNT_ENABLED" : "ACCOUNT_DISABLED",
                actingAdminId,
                saved.getId(),
                enabled ? "Account enabled by Admin" : "Account disabled by Admin"
        );
        return AdminUserResponse.from(saved);
    }

    private Specification<User> filter(String search, Role role, Boolean enabled) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(search)) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(root.get("mobile"), like)
                ));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
