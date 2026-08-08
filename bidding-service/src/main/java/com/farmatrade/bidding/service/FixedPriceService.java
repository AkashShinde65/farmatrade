package com.farmatrade.bidding.service;

import com.farmatrade.bidding.dto.AuctionResultResponse;
import com.farmatrade.bidding.entity.Auction;
import com.farmatrade.bidding.exception.BidValidationException;
import com.farmatrade.bidding.repository.AuctionRepository;
import com.farmatrade.bidding.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Handles Buy-Now purchases for FIXED_PRICE lots: validates eligibility,
 * then delegates the actual close-out (Sale generation, Lot/Billing
 * notification, winner broadcast) to AuctionClosingService so both the
 * scheduler-driven and buy-now-driven paths share one implementation.
 */
@Service
@RequiredArgsConstructor
public class FixedPriceService {

    private static final Logger log = LoggerFactory.getLogger(FixedPriceService.class);

    private final AuctionRepository auctionRepository;
    private final SaleRepository saleRepository;
    private final BidValidationService bidValidationService;
    private final AuctionClosingService auctionClosingService;

    @Transactional
    public AuctionResultResponse buyNow(Long lotId, Long buyerId, String idempotencyKey) {
        Auction auction = auctionRepository.findByLotId(lotId)
                .orElseThrow(() -> new BidValidationException("No auction found for lot " + lotId));

        if (auction.getStatus() == Auction.AuctionStatus.SOLD) {
            // Idempotent replay: lot is already sold, return the existing result
            // instead of erroring, so a retried client request doesn't fail.
            log.info("Buy-Now replay detected for already-SOLD lot {} (idempotencyKey={})",
                    lotId, idempotencyKey);
            return toResult(auction);
        }

        bidValidationService.validateBuyNow(auction);

        log.info("Buy-Now purchase for lot {} by buyer {} at fixed price {}",
                lotId, buyerId, auction.getFixedPrice());

        auctionClosingService.closeBuyNow(auction, buyerId, auction.getFixedPrice());

        return toResult(auction);
    }

    private AuctionResultResponse toResult(Auction auction) {
        Long saleId = saleRepository.findByLotId(auction.getLotId())
                .map(sale -> sale.getSaleId())
                .orElse(null);

        return new AuctionResultResponse(
                auction.getLotId(),
                auction.getStatus(),
                auction.getCurrentHighestBidderId(),
                auction.getCurrentHighestBid(),
                auction.getEndTime(),
                Instant.now(),
                saleId
        );
    }
}
