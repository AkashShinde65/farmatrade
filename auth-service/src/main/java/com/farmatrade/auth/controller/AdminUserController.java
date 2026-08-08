package com.farmatrade.auth.controller;

import com.farmatrade.auth.dto.AccountStatusRequest;
import com.farmatrade.auth.dto.AdminUserResponse;
import com.farmatrade.auth.entity.Role;
import com.farmatrade.auth.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Users", description = "ADMIN-only user management APIs")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(
            summary = "List and search users",
            description = "ADMIN-only paginated list with search, role and enabled filters.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Users returned"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                    @ApiResponse(responseCode = "403", description = "Authenticated user is not ADMIN")
            }
    )
    public Page<AdminUserResponse> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled,
            Pageable pageable
    ) {
        return adminUserService.listUsers(search, role, enabled, pageable);
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Enable or disable an account",
            description = "ADMIN-only status change. Admins cannot disable themselves or the last active Admin.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status updated"),
                    @ApiResponse(responseCode = "400", description = "Invalid request"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                    @ApiResponse(responseCode = "403", description = "Not allowed")
            }
    )
    public ResponseEntity<AdminUserResponse> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody AccountStatusRequest request,
            Authentication authentication
    ) {
        Long actingAdminId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(adminUserService.setEnabled(actingAdminId, id, request.enabled()));
    }
}
