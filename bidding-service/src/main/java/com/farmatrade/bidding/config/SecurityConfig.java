package com.farmatrade.bidding.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Security configuration for the Bidding Service.
 *
 * Design decisions:
 *  - Stateless OAuth2 Resource Server: the Auth Service (P1) is the single
 *    token issuer. This service NEVER issues or validates passwords itself;
 *    it only validates JWTs signed by the Auth Service, fetched via JWKS.
 *  - Roles are read from the single "role" claim (a String, not an array)
 *    issued by the Auth Service and mapped to a Spring Security
 *    GrantedAuthority prefixed with "ROLE_" so that @PreAuthorize
 *    ("hasRole('BUYER')") works out of the box on controller methods.
 *  - Audience validation is enforced in addition to the default issuer/
 *    expiry validation, so a token minted for a different downstream API
 *    cannot be replayed against the Bidding Service.
 *  - CSRF is disabled because this is a stateless, token-based, non-browser
 *    -session API (standard practice for REST + JWT).
 *  - Actuator health/info and the WebSocket handshake endpoint are public;
 *    everything else requires a valid JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${auth.jwks-uri}")
    private String jwksUri;

    @Value("${auth.issuer-uri}")
    private String issuerUri;

    @Value("${auth.audience}")
    private String audience;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                    .frameOptions(frame -> frame.deny())
                    .contentTypeOptions(withDefaults -> {})
                    .httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000))
                    .referrerPolicy(referrer -> referrer
                            .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/actuator/health",
                            "/actuator/info",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**",
                            "/ws/**"
                    ).permitAll()
                    .requestMatchers("/internal/**").hasRole("SERVICE")
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
     * JWT decoder backed by the Auth Service's published JWKS endpoint.
     * Additionally validates issuer + audience so tokens are only accepted
     * if they were minted specifically for the farmatrade-api audience.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> audienceValidator = audienceValidator();
        OAuth2TokenValidator<Jwt> combinedValidator =
                new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator);

        jwtDecoder.setJwtValidator(combinedValidator);
        return jwtDecoder;
    }

    private OAuth2TokenValidator<Jwt> audienceValidator() {
        return jwt -> {
            List<String> audiences = jwt.getAudience();
            if (audiences != null && audiences.contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Required audience '" + audience + "' is missing", null));
        };
    }

    /**
     * Maps the "role" claim (a single String, e.g. "BUYER" - NOT an array)
     * issued by the Auth Service into a Spring Security ROLE_ authority,
     * enabling @PreAuthorize("hasRole('BUYER')") checks.
     *
     * BUGFIX: this previously called jwt.getClaimAsStringList("roles")
     * (plural claim name, array type). auth-service actually issues a
     * single "role" string claim. Every authenticated user therefore
     * resolved to zero granted authorities, and every @PreAuthorize
     * check on both placing a bid and buy-now returned 403 for every
     * legitimately logged-in buyer. Fixed to read the correct claim name
     * and type.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
        defaultConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> defaultAuthorities = defaultConverter.convert(jwt);

            String role = jwt.getClaimAsString("role");
            Stream<GrantedAuthority> roleAuthority = role == null || role.isBlank()
                    ? Stream.empty()
                    : Stream.of(new SimpleGrantedAuthority("ROLE_" + role));

            return Stream.concat(
                    defaultAuthorities == null ? Stream.empty() : defaultAuthorities.stream(),
                    roleAuthority
            ).collect(Collectors.toSet());
        });
        return converter;
    }

    /**
     * CORS is intentionally permissive on methods/headers but restricted to
     * an explicit, environment-driven origin allow-list in production
     * deployments (configure via a dedicated CorsProperties bean / env var
     * when moving beyond local development).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "https://*.farmatrade.local"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
        configuration.setExposedHeaders(List.of("X-Correlation-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
