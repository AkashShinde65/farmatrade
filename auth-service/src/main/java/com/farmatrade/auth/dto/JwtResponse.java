package com.farmatrade.auth.dto;

public record JwtResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserRegistrationResponse user
) {
    public JwtResponse(String accessToken, long expiresInSeconds, UserRegistrationResponse user) {
        this(accessToken, "Bearer", expiresInSeconds, user);
    }
}
