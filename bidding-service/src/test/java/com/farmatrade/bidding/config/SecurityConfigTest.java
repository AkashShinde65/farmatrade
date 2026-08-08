package com.farmatrade.bidding.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the P0 bug: auth-service issues a single "role"
 * string claim (e.g. "BUYER"), not a "roles" array. The converter must
 * read the correct claim name/type or every authenticated user resolves
 * to zero granted authorities and every @PreAuthorize check 403s.
 */
class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void converter_grantsRoleAuthority_fromSingularRoleStringClaim() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        Jwt jwt = buildJwt(Map.of("role", "BUYER", "sub", "101"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt).getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_BUYER");
    }

    @Test
    void converter_grantsNoRoleAuthority_whenLegacyPluralRolesArrayClaimIsSent() {
        // Guards against silently regressing back to the old (wrong) claim shape.
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        Jwt jwt = buildJwt(Map.of("roles", java.util.List.of("BUYER"), "sub", "101"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt).getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .doesNotContain("ROLE_BUYER");
    }

    @Test
    void converter_grantsNoRoleAuthority_whenRoleClaimMissing() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        Jwt jwt = buildJwt(Map.of("sub", "101"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt).getAuthorities();

        assertThat(authorities).noneMatch(a -> a.getAuthority().startsWith("ROLE_"));
    }

    private Jwt buildJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claims(c -> c.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
