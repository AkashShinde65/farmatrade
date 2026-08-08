package com.farmatrade.bidding.controller;

import com.farmatrade.bidding.dto.AuctionResultResponse;
import com.farmatrade.bidding.dto.BidResponse;
import com.farmatrade.bidding.dto.PlaceBidRequest;
import com.farmatrade.bidding.entity.Auction;
import com.farmatrade.bidding.service.AuctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Public-facing REST API for the live-auction lifecycle: create auction,
 * place a bid, and fetch the current/final auction result.
 */
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
@Tag(name = "Auctions", description = "Live auction and bidding operations")
public class AuctionController {

    private final AuctionService auctionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    @Operation(summary = "Create a new auction for a lot")
    public ResponseEntity<Auction> createAuction(@Valid @RequestBody CreateAuctionRequest request) {
        Auction auction = auctionService.createAuction(
                request.lotId(),
                request.farmerId(),
                request.lotType(),
                request.startingPrice(),
                request.fixedPrice(),
                request.endTime()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(auction);
    }

    @PostMapping("/{lotId}/bids")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Place a bid on an active auction")
    public ResponseEntity<BidResponse> placeBid(@PathVariable Long lotId,
                                                 @Valid @RequestBody PlaceBidRequest request,
                                                 JwtAuthenticationToken authentication) {
        Long buyerId = extractUserId(authentication);
        BidResponse response = auctionService.placeBid(lotId, buyerId, request.amount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{lotId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the current or final result of an auction")
    public ResponseEntity<AuctionResultResponse> getAuctionResult(@PathVariable Long lotId) {
        return ResponseEntity.ok(auctionService.getAuctionResult(lotId));
    }

    /**
     * Resolves the internal buyerId from the authenticated JWT. auth-service places the numeric
     * platform user id in the standard "sub" claim (see JwtUtil.generateToken) -- never trust a
     * buyerId supplied in the request body itself.
     */
    private Long extractUserId(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        String subject = jwt.getSubject();
        if (subject == null) {
            throw new IllegalStateException("JWT is missing required 'sub' claim");
        }
        return Long.valueOf(subject);
    }

    public record CreateAuctionRequest(
            @NotNull Long lotId,
            @NotNull Long farmerId,
            @NotNull Auction.LotType lotType,
            @NotNull @Positive BigDecimal startingPrice,
            BigDecimal fixedPrice,
            @NotNull Instant endTime
    ) {
    }
}
