package com.farmatrade.bidding.controller;

import com.farmatrade.bidding.config.SecurityConfig;
import com.farmatrade.bidding.dto.AuctionResultResponse;
import com.farmatrade.bidding.dto.BidResponse;
import com.farmatrade.bidding.dto.PlaceBidRequest;
import com.farmatrade.bidding.entity.Auction;
import com.farmatrade.bidding.exception.AuctionClosedException;
import com.farmatrade.bidding.service.AuctionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer test for AuctionController. Imports the REAL SecurityConfig
 * (rather than relying on Spring Boot's default test security) so
 * @PreAuthorize role checks are genuinely exercised, not bypassed.
 * SecurityMockMvcRequestPostProcessors.jwt() injects a JwtAuthenticationToken
 * directly into the SecurityContext, so no real JWKS call happens.
 */
@WebMvcTest(AuctionController.class)
@Import(SecurityConfig.class)
class AuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuctionService auctionService;

    @Test
    void placeBid_returnsOk_whenCallerHasBuyerRole() throws Exception {
        BidResponse response = new BidResponse(
                1L, 1001L, 101L, BigDecimal.valueOf(1500),
                Instant.now(), BigDecimal.valueOf(1500), Instant.now().plusSeconds(600), false);
        when(auctionService.placeBid(anyLong(), anyLong(), any(BigDecimal.class))).thenReturn(response);

        PlaceBidRequest request = new PlaceBidRequest(BigDecimal.valueOf(1500));

        mockMvc.perform(post("/api/auctions/1001/bids")
                        .with(jwt()
                                .jwt(builder -> builder.claim("userId", "101"))
                                .authorities(new SimpleGrantedAuthority("ROLE_BUYER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotId").value(1001))
                .andExpect(jsonPath("$.buyerId").value(101));
    }

    @Test
    void placeBid_returnsForbidden_whenCallerLacksBuyerRole() throws Exception {
        PlaceBidRequest request = new PlaceBidRequest(BigDecimal.valueOf(1500));

        mockMvc.perform(post("/api/auctions/1001/bids")
                        .with(jwt()
                                .jwt(builder -> builder.claim("userId", "101"))
                                .authorities(new SimpleGrantedAuthority("ROLE_FARMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void placeBid_returnsUnauthorized_whenNoTokenProvided() throws Exception {
        PlaceBidRequest request = new PlaceBidRequest(BigDecimal.valueOf(1500));

        mockMvc.perform(post("/api/auctions/1001/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void placeBid_returnsBadRequest_whenAmountIsMissing() throws Exception {
        mockMvc.perform(post("/api/auctions/1001/bids")
                        .with(jwt()
                                .jwt(builder -> builder.claim("userId", "101"))
                                .authorities(new SimpleGrantedAuthority("ROLE_BUYER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeBid_returnsConflict_whenAuctionAlreadyClosed() throws Exception {
        when(auctionService.placeBid(anyLong(), anyLong(), any(BigDecimal.class)))
                .thenThrow(new AuctionClosedException("Auction for lot 1001 has already expired"));

        PlaceBidRequest request = new PlaceBidRequest(BigDecimal.valueOf(1500));

        mockMvc.perform(post("/api/auctions/1001/bids")
                        .with(jwt()
                                .jwt(builder -> builder.claim("userId", "101"))
                                .authorities(new SimpleGrantedAuthority("ROLE_BUYER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getAuctionResult_returnsOk_forAnyAuthenticatedUser() throws Exception {
        AuctionResultResponse result = new AuctionResultResponse(
                1001L, Auction.AuctionStatus.ACTIVE, null, null, Instant.now().plusSeconds(600), null, null);
        when(auctionService.getAuctionResult(1001L)).thenReturn(result);

        mockMvc.perform(get("/api/auctions/1001")
                        .with(jwt()
                                .jwt(builder -> builder.claim("userId", "101"))
                                .authorities(new SimpleGrantedAuthority("ROLE_BUYER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotId").value(1001));
    }
}
