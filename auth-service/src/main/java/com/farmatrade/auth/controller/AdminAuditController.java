package com.farmatrade.auth.controller;

import com.farmatrade.auth.dto.AuditEventResponse;
import com.farmatrade.auth.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-events")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Audit", description = "ADMIN-only security audit events")
public class AdminAuditController {
    private final AuditService auditService;

    public AdminAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(
            summary = "List security audit events",
            description = "ADMIN-only paginated audit view. Records never include passwords, JWTs, Aadhaar values, Aadhaar hashes, peppers or private keys.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Audit events returned"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                    @ApiResponse(responseCode = "403", description = "Authenticated user is not ADMIN")
            }
    )
    public Page<AuditEventResponse> list(Pageable pageable) {
        return auditService.list(pageable);
    }
}
