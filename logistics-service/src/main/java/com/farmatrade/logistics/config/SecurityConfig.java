package com.farmatrade.logistics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * CORRECTED 2026-07-30 -- the original single-chain design (InternalServiceTokenFilter added via
 * addFilterBefore(..., BearerTokenAuthenticationFilter.class) on the same chain as the OAuth2
 * resource server) does not work: BearerTokenAuthenticationFilter unconditionally tries to decode
 * any "Authorization: Bearer ..." header as a JWT regardless of whether an earlier filter already
 * populated the SecurityContext, verified empirically (BadJwtException: "Missing dot
 * delimiter(s)") against billing-service and lot-service using the same original pattern.
 *
 * Fixed with two separate SecurityFilterChain beans matched by path/method, so the shared static
 * token never reaches the JWT decoder. Unlike billing-service/lot-service, the bidding-service ->
 * logistics-service call is not under an /internal/** prefix -- it is POST /api/logistics/requests,
 * while GET /api/logistics/requests (list-by-buyer) on the same base path is a real end-user
 * endpoint that must stay on the JWT chain. The internal chain is therefore matched on method +
 * exact path, not a path prefix.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${auth.jwt.issuer}")
    private String expectedIssuer;

    @Value("${auth.jwt.audience}")
    private String expectedAudience;

    @Bean
    @Order(1)
    public SecurityFilterChain internalServiceFilterChain(HttpSecurity http, InternalServiceTokenFilter internalServiceTokenFilter) throws Exception {
        http
                .securityMatcher(new AntPathRequestMatcher("/api/logistics/requests", "POST"))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("SERVICE"))
                .addFilterBefore(internalServiceTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(new AntPathRequestMatcher("/actuator/health"),
                                new AntPathRequestMatcher("/actuator/info"),
                                new AntPathRequestMatcher("/error")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/logistics/truck-bookings/*/advance"))
                        .hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );
        return http.build();
    }

    /**
     * logistics-service had no CORS configuration at all before this -- every browser call the
     * frontend makes to the buyer-facing endpoints (accept/decline, cold-storage, truck fleet)
     * would be blocked regardless of backend correctness. Mirrors the fix applied to lot-service
     * and billing-service the same day.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${logistics.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> withIssuer = new JwtClaimValidator<String>("iss", expectedIssuer::equals);
        OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>(
                "aud", aud -> aud != null && aud.contains(expectedAudience));
        OAuth2TokenValidator<Jwt> defaultValidators = JwtValidators.createDefault();

        OAuth2TokenValidator<Jwt> combined =
                new DelegatingOAuth2TokenValidator<>(defaultValidators, withIssuer, withAudience);

        decoder.setJwtValidator(combined);
        return decoder;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object role = jwt.getClaims().get("role");
            if (role == null) {
                return List.of();
            }
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            return authorities;
        });
        return converter;
    }
}
