package com.farmatrade.bidding.service;

import com.farmatrade.bidding.entity.Auction;
import com.farmatrade.bidding.exception.AuctionClosedException;
import com.farmatrade.bidding.exception.BidValidationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Encapsulates the auction bidding rules so AuctionService stays focused
 * on orchestration:
 *
 *  - Auction must be ACTIVE or EXTENDED
 *  - Auction end time must not have passed
 *  - Bid amount must be strictly greater than the current highest bid
 *    (or the starting price if no bids yet)
 *
 * Buyer authentication itself is enforced upstream by Spring Security
 * (@PreAuthorize("hasRole('BUYER')")); this service assumes the caller
 * is already an authenticated buyer and only checks auction/bid-specific
 * business rules.
 */
@Service
public class BidValidationService {

    public void validatePlaceBid(Auction auction, BigDecimal bidAmount) {
        if (auction.getStatus() != Auction.AuctionStatus.ACTIVE
                && auction.getStatus() != Auction.AuctionStatus.EXTENDED) {
            throw new AuctionClosedException(
                    "Auction for lot " + auction.getLotId() + " is not open for bidding (status="
                            + auction.getStatus() + ")");
        }

        if (auction.getEndTime().isBefore(Instant.now())) {
            throw new AuctionClosedException(
                    "Auction for lot " + auction.getLotId() + " has already expired");
        }

        BigDecimal floor = auction.getCurrentHighestBid() != null
                ? auction.getCurrentHighestBid()
                : auction.getStartingPrice();

        if (bidAmount.compareTo(floor) <= 0) {
            throw new BidValidationException(
                    "Bid amount " + bidAmount + " must be greater than the current highest bid " + floor);
        }
    }

    public void validateBuyNow(Auction auction) {
        if (auction.getLotType() != Auction.LotType.FIXED_PRICE) {
            throw new BidValidationException(
                    "Lot " + auction.getLotId() + " is not a FIXED_PRICE lot and cannot be bought now");
        }

        if (auction.getStatus() == Auction.AuctionStatus.SOLD
                || auction.getStatus() == Auction.AuctionStatus.CLOSED
                || auction.getStatus() == Auction.AuctionStatus.CANCELLED) {
            throw new AuctionClosedException(
                    "Lot " + auction.getLotId() + " is no longer available (status=" + auction.getStatus() + ")");
        }
    }

    /** True if this bid falls within the anti-sniping window before auction end. */
    public boolean isWithinAntiSnipeWindow(Auction auction, int windowSeconds) {
        Instant windowStart = auction.getEndTime().minusSeconds(windowSeconds);
        return Instant.now().isAfter(windowStart);
    }
}
