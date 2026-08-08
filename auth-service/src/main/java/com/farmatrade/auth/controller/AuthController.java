package com.farmatrade.auth.controller;

import com.farmatrade.auth.dto.JwtResponse;
import com.farmatrade.auth.dto.LoginRequest;
import com.farmatrade.auth.dto.UserProfileResponse;
import com.farmatrade.auth.dto.UserRegistrationResponse;
import com.farmatrade.auth.entity.Role;
import com.farmatrade.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Role-specific login and current-user profile APIs")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login/farmer")
    @Operation(
            summary = "Farmer login",
            description = "Authenticates only FARMER accounts (by email or phone number) and returns an RS256 JWT.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login successful"),
                    @ApiResponse(responseCode = "400", description = "Invalid request"),
                    @ApiResponse(responseCode = "401", description = "Invalid credentials or wrong login portal")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = "{\"identifier\":\"farmer@example.com\",\"password\":\"StrongPass1!\"}")))
    )
    public ResponseEntity<JwtResponse> loginFarmer(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request, Role.FARMER));
    }

    @PostMapping("/login/buyer")
    @Operation(summary = "Buyer login", description = "Authenticates only BUYER accounts and returns an RS256 JWT.")
    public ResponseEntity<JwtResponse> loginBuyer(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request, Role.BUYER));
    }

    @PostMapping("/login/admin")
    @Operation(summary = "Admin login", description = "Authenticates only ADMIN accounts and returns an RS256 JWT.")
    public ResponseEntity<JwtResponse> loginAdmin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request, Role.ADMIN));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Current user profile",
            description = "Returns the authenticated user's safe profile only.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Safe profile returned"),
                    @ApiResponse(responseCode = "401", description = "Missing, invalid, expired, disabled or locked token")
            }
    )
    public ResponseEntity<UserProfileResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.currentUser((Long) authentication.getPrincipal()));
    }
}
