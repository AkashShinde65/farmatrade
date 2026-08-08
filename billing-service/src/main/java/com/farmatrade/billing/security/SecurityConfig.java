package com.farmatrade.billing.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${auth.jwt.issuer}")
    private String issuerUri;

    @Value("${auth.jwt.audience}")
    private String audience;

    /**
     * CORRECTED 2026-07-30 -- the previous single-chain design (InternalServiceTokenFilter
     * added via addFilterBefore(..., BearerTokenAuthenticationFilter.class) on the SAME chain
     * as the OAuth2 resource server) does NOT work: BearerTokenAuthenticationFilter still
     * unconditionally tries to decode ANY "Authorization: Bearer ..." header as a JWT,
     * regardless of whether an earlier filter already populated the SecurityContext. Verified
     * empirically -- a request carrying the shared static token still 401'd with
     * BadJwtException: "Invalid JWT serialization: Missing dot delimiter(s)", proving the static
     * token was being fed into the JWT decoder anyway.
     *
     * The fix is two entirely separate SecurityFilterChain beans, matched by path
     * (securityMatcher), so /internal/** requests are handled by a chain that never installs
     * oauth2ResourceServer()/BearerTokenAuthenticationFilter in the first place. Spring Security
     * picks the first chain whose matcher matches and uses ONLY that chain -- the JWT decoder is
     * never invoked for /internal/** at all.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain internalServiceFilterChain(HttpSecurity http, InternalServiceTokenFilter internalServiceTokenFilter) throws Exception {
        http
                .securityMatcher("/internal/**")
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
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/health",
                                "/error",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/webhook/**"
                        ).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    /**
     * billing-service had no CORS configuration at all before this -- every browser call the
     * frontend makes to /invoice/** and /payment/** would be blocked regardless of backend
     * correctness. Mirrors the fix applied to lot-service and logistics-service the same day.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${billing.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String allowedOrigins
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

    /**
     * Previously used Customizer.withDefaults(), which only validates signature + expiry.
     * Adds issuer + audience checks so a token minted for a different issuer/downstream API
     * cannot be replayed against Billing Service -- matching the pattern already fixed in
     * bidding-service's SecurityConfig.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> audienceValidator = audienceValidator();

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
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
     * Maps auth-service's single "role" string claim (e.g. "ADMIN") into a Spring Security
     * ROLE_ authority. Previously no converter was configured at all, so any future
     * @PreAuthorize("hasRole(...)") check here would have silently never matched -- the same bug
     * found and fixed in bidding-service's SecurityConfig.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            Stream<GrantedAuthority> roleAuthority = (role == null || role.isBlank())
                    ? Stream.empty()
                    : Stream.of(new SimpleGrantedAuthority("ROLE_" + role));
            return roleAuthority.collect(Collectors.toSet());
        });
        return converter;
    }
}
