package com.farmatrade.auth.dto;

import com.farmatrade.auth.entity.Role;
import com.farmatrade.auth.entity.User;
import java.time.Instant;

/**
 * Admin-only view of a user, kept separate from UserProfileResponse (returned by GET /api/auth/me)
 * specifically so it can include the Aadhaar number -- a regular user's own self-service profile
 * must not gain that field just because this one needs it.
 */
public record AdminUserResponse(
        Long id,
        String fullName,
        String email,
        String mobile,
        String aadhaar,
        Role role,
        boolean enabled,
        Instant createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getMobile(),
                user.getAadhaarHash(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
