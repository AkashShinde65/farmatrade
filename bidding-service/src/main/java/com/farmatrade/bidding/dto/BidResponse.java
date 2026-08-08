package com.farmatrade.bidding.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response returned to the caller immediately after a bid is accepted.
 */
public record BidResponse(
        Long bidId,
        Long lotId,
        Long buyerId,
        BigDecimal amount,
        Instant bidTime,
        BigDecimal currentHighestBid,
        Instant auctionEndTime,
        boolean auctionExtended
) {
}
