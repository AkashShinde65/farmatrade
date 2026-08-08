package com.farmatrade.bidding.dto;

import com.farmatrade.bidding.entity.Auction;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Current or final state of an auction, returned by the "get auction"
 * endpoint and used internally when broadcasting the closed/winner event.
 */
public record AuctionResultResponse(
        Long lotId,
        Auction.AuctionStatus status,
        Long winnerId,
        BigDecimal winningAmount,
        Instant endTime,
        Instant closedAt,
        Long saleId
) {
}
