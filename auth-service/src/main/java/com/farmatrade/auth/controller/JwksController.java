package com.farmatrade.auth.controller;

import com.farmatrade.auth.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "JWKS", description = "Public RSA public key set for JWT verification")
public class JwksController {
    private final JwtUtil jwtUtil;

    public JwksController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "JWKS", description = "Returns the public RSA key used to verify RS256 access tokens.")
    public Map<String, Object> jwks() {
        return jwtUtil.jwks();
    }
}
