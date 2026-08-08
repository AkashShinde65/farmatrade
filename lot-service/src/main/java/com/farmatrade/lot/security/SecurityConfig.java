package com.farmatrade.lot.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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

/**
 * Added 2026-07-30 -- lot-service previously had no security configuration at all, meaning
 * every /api/lots/** endpoint was completely open. This validates JWTs against auth-service's
 * real JWKS endpoint (/.well-known/jwks.json), checks issuer + audience, and maps the single
 * "role" string claim auth-service actually issues into a ROLE_ authority -- matching the
 * pattern already fixed in bidding-service's and billing-service's SecurityConfig.
 *
 * CORRECTED same day -- a single shared chain with InternalServiceTokenFilter added via
 * addFilterBefore(..., BearerTokenAuthenticationFilter.class) does NOT work:
 * BearerTokenAuthenticationFilter still unconditionally tries to decode ANY bearer token as a
 * JWT regardless of an existing SecurityContext, verified empirically (BadJwtException:
 * "Missing dot delimiter(s)" on the shared static token). Fixed by splitting into two chains --
 * /internal/** never installs oauth2ResourceServer()/BearerTokenAuthenticationFilter at all.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${auth.jwt.issuer}")
    private String issuerUri;

    @Value("${auth.jwt.audience}")
    private String audience;

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

    /**
     * Read-only lot browsing is public by design (see LotServiceClient's own comment on the
     * bidding-service side: "getLot() calls the public GET /api/lots/{id}") -- bidding-service's
     * AuctionClosingService calls this with the shared internal static token (not a real JWT) to
     * fetch cropName/quantity/locationName when building the post-sale logistics notification.
     *
     * This MUST be its own chain that never installs oauth2ResourceServer(), not just a
     * permitAll() rule inside the default chain -- putting it there was tried first and still
     * 401'd empirically: BearerTokenAuthenticationFilter runs BEFORE the authorization decision
     * and unconditionally tries to decode ANY "Authorization: Bearer ..." header as a JWT,
     * short-circuiting with 401 via the AuthenticationEntryPoint on failure before
     * AuthorizationFilter's permitAll() rule is ever consulted. A permitAll() rule only protects
     * a route when no Authorization header is present (or the resource server filter isn't
     * installed at all on that chain) -- it does NOT override a failed authentication attempt.
     * Same root cause as the internal-token chains above, different symptom.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain publicLotBrowsingFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/lots/**", "GET"))
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info", "/error").permitAll()
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
     * lot-service had no CORS configuration at all before this -- the frontend (a browser app on
     * a different origin/port) would have every request blocked regardless of whether the backend
     * logic was correct. Mirrors auth-service's env-var-driven allow-list, but includes PUT/DELETE
     * since lot-service's /api/lots/{id} update and cancel endpoints need them (auth-service only
     * ever needed GET/POST/PATCH).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${lot.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String allowedOrigins
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

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> audienceValidator = audienceValidator();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return decoder;
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
