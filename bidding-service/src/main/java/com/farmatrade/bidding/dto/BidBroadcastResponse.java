package com.farmatrade.bidding.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Envelope broadcast over STOMP to /topic/auction/{lotId} for every
 * real-time auction event. eventType tells subscribed clients how to
 * interpret the payload without needing separate DTO classes per event.
 */
public record BidBroadcastResponse(
        EventType eventType,
        Long lotId,
        BigDecimal highestBid,
        Long highestBidderId,
        Instant auctionEndTime,
        Long winnerId,
        BigDecimal winningAmount,
        Instant timestamp
) {
    public enum EventType {
        NEW_BID,
        AUCTION_EXTENDED,
        AUCTION_CLOSED,
        WINNER_ANNOUNCED
    }
}
