package com.farmatrade.auth.controller;

import com.farmatrade.auth.dto.RegisterRequest;
import com.farmatrade.auth.dto.UserRegistrationResponse;
import com.farmatrade.auth.entity.Role;
import com.farmatrade.auth.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/register")
@Tag(name = "Registration", description = "Fixed-role registration APIs")
public class RegistrationController {
    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/farmer")
    @Operation(
            summary = "Register Farmer",
            description = "Public registration endpoint that always assigns FARMER. Aadhaar values in examples are mock checksum-valid data.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Registered"),
                    @ApiResponse(responseCode = "400", description = "Invalid input or Aadhaar checksum"),
                    @ApiResponse(responseCode = "409", description = "Duplicate email, mobile or Aadhaar hash")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = "{\"fullName\":\"Farmer One\",\"email\":\"farmer@example.com\",\"mobile\":\"9000000001\",\"password\":\"StrongPass1!\",\"aadhaar\":\"900000000002\"}")))
    )
    public ResponseEntity<UserRegistrationResponse> registerFarmer(@Valid @RequestBody RegisterRequest request) {
        UserRegistrationResponse response = registrationService.register(request, Role.FARMER, null);
        return ResponseEntity.created(URI.create("/api/auth/users/" + response.id())).body(response);
    }

    @PostMapping("/buyer")
    @Operation(summary = "Register Buyer", description = "Public registration endpoint that always assigns BUYER.")
    public ResponseEntity<UserRegistrationResponse> registerBuyer(@Valid @RequestBody RegisterRequest request) {
        UserRegistrationResponse response = registrationService.register(request, Role.BUYER, null);
        return ResponseEntity.created(URI.create("/api/auth/users/" + response.id())).body(response);
    }

    @PostMapping("/admin")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Register Admin",
            description = "ADMIN-only endpoint that always assigns ADMIN and records the creator Admin ID.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Admin registered"),
                    @ApiResponse(responseCode = "400", description = "Invalid input or Aadhaar checksum"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                    @ApiResponse(responseCode = "403", description = "Authenticated user is not ADMIN"),
                    @ApiResponse(responseCode = "409", description = "Duplicate email, mobile or Aadhaar hash")
            }
    )
    public ResponseEntity<UserRegistrationResponse> registerAdmin(
            @Valid @RequestBody RegisterRequest request,
            Authentication authentication
    ) {
        Long creatorAdminId = (Long) authentication.getPrincipal();
        UserRegistrationResponse response = registrationService.register(request, Role.ADMIN, creatorAdminId);
        return ResponseEntity.created(URI.create("/api/auth/users/" + response.id())).body(response);
    }
}
