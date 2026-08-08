package com.farmatrade.auth.dto;

import com.farmatrade.auth.entity.Role;
import com.farmatrade.auth.entity.User;
import java.time.Instant;

public record UserRegistrationResponse(
        Long id,
        String fullName,
        String email,
        String mobile,
        Role role,
        Long createdByAdminId,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserRegistrationResponse from(User user) {
        return new UserRegistrationResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getMobile(),
                user.getRole(),
                user.getCreatedByAdminId(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
