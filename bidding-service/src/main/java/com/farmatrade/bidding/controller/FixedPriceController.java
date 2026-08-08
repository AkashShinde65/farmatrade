package com.farmatrade.bidding.controller;

import com.farmatrade.bidding.dto.AuctionResultResponse;
import com.farmatrade.bidding.dto.BuyNowRequest;
import com.farmatrade.bidding.service.FixedPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public-facing REST API for Buy-Now purchases on FIXED_PRICE lots.
 */
@RestController
@RequestMapping("/api/fixed-price")
@RequiredArgsConstructor
@Tag(name = "Fixed Price", description = "Buy-Now operations for fixed-price lots")
public class FixedPriceController {

    private final FixedPriceService fixedPriceService;

    @PostMapping("/{lotId}/buy-now")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Immediately purchase a FIXED_PRICE lot")
    public ResponseEntity<AuctionResultResponse> buyNow(@PathVariable Long lotId,
                                                          @Valid @RequestBody BuyNowRequest request,
                                                          JwtAuthenticationToken authentication) {
        Long buyerId = extractUserId(authentication);
        AuctionResultResponse result = fixedPriceService.buyNow(lotId, buyerId, request.idempotencyKey());
        return ResponseEntity.ok(result);
    }

    private Long extractUserId(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        String subject = jwt.getSubject();
        if (subject == null) {
            throw new IllegalStateException("JWT is missing required 'sub' claim");
        }
        return Long.valueOf(subject);
    }
}
