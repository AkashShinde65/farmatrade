package com.farmatrade.auth.dto;

import com.farmatrade.auth.entity.Role;
import com.farmatrade.auth.entity.User;
import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        String mobile,
        Role role,
        boolean enabled,
        Instant createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getMobile(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
