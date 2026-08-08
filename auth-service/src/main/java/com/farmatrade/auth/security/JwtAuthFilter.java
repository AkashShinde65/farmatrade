package com.farmatrade.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import com.farmatrade.auth.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final Clock clock;

    public JwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository, Clock clock) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            try {
                JwtUtil.ValidatedJwt jwt = jwtUtil.validate(header.substring(7));
                userRepository.findById(jwt.userId())
                        .filter(user -> user.isEnabled())
                        .filter(user -> user.getLockedUntil() == null || !user.getLockedUntil().isAfter(clock.instant()))
                        .ifPresent(user -> {
                            var auth = new UsernamePasswordAuthenticationToken(
                                    jwt.userId(),
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + jwt.role()))
                            );
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        });
            } catch (IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
