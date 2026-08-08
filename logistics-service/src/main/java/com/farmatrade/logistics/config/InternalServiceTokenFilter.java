package com.farmatrade.logistics.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates trusted internal callers (bidding-service, calling POST /api/logistics/requests)
 * via a shared static bearer token instead of a real JWT -- there is no client-credentials flow
 * from auth-service yet (see bidding-service's WebClientConfig Javadoc for the full rationale).
 * If the Authorization header exactly matches internal-service.token, the caller is granted
 * ROLE_SERVICE and the request proceeds without going through JWT validation. Any other
 * Authorization header (or none) falls through to the normal OAuth2 resource server filter so
 * real end-user JWTs keep working. Mirrors lot-service's and billing-service's identical filter.
 */
@Component
public class InternalServiceTokenFilter extends OncePerRequestFilter {

    private final String internalServiceToken;

    public InternalServiceTokenFilter(@Value("${internal-service.token}") String internalServiceToken) {
        this.internalServiceToken = internalServiceToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.equals("Bearer " + internalServiceToken)) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "internal-service", null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
