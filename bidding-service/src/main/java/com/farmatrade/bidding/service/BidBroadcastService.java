package com.farmatrade.bidding.service;

import com.farmatrade.bidding.dto.BidBroadcastResponse;
import com.farmatrade.bidding.entity.Auction;
import com.farmatrade.bidding.websocket.BidMessageBroker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Builds the four real-time event payloads defined by the requirements
 * (NEW_BID, AUCTION_EXTENDED, AUCTION_CLOSED, WINNER_ANNOUNCED) and hands
 * them to BidMessageBroker for delivery over STOMP.
 */
@Service
@RequiredArgsConstructor
public class BidBroadcastService {

    private final BidMessageBroker messageBroker;

    public void broadcastNewBid(Auction auction) {
        messageBroker.publish(new BidBroadcastResponse(
                BidBroadcastResponse.EventType.NEW_BID,
                auction.getLotId(),
                auction.getCurrentHighestBid(),
                auction.getCurrentHighestBidderId(),
                auction.getEndTime(),
                null,
                null,
                Instant.now()
        ));
    }

    public void broadcastAuctionExtended(Auction auction) {
        messageBroker.publish(new BidBroadcastResponse(
                BidBroadcastResponse.EventType.AUCTION_EXTENDED,
                auction.getLotId(),
                auction.getCurrentHighestBid(),
                auction.getCurrentHighestBidderId(),
                auction.getEndTime(),
                null,
                null,
                Instant.now()
        ));
    }

    public void broadcastAuctionClosed(Auction auction) {
        messageBroker.publish(new BidBroadcastResponse(
                BidBroadcastResponse.EventType.AUCTION_CLOSED,
                auction.getLotId(),
                auction.getCurrentHighestBid(),
                auction.getCurrentHighestBidderId(),
                auction.getEndTime(),
                null,
                null,
                Instant.now()
        ));
    }

    public void broadcastWinner(Long lotId, Long winnerId, BigDecimal winningAmount) {
        messageBroker.publish(new BidBroadcastResponse(
                BidBroadcastResponse.EventType.WINNER_ANNOUNCED,
                lotId,
                null,
                null,
                null,
                winnerId,
                winningAmount,
                Instant.now()
        ));
    }
}
