package com.farmatrade.bidding.service;

import com.farmatrade.bidding.dto.BidResponse;
import com.farmatrade.bidding.entity.Auction;
import com.farmatrade.bidding.entity.Bid;
import com.farmatrade.bidding.exception.BidValidationException;
import com.farmatrade.bidding.repository.AuctionRepository;
import com.farmatrade.bidding.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Executes exactly one attempt of the "place bid" critical section inside
 * a real database transaction.
 *
 * This logic is intentionally NOT a method on AuctionService: calling an
 * @Transactional method from another method in the SAME class (self
 * -invocation) bypasses Spring's transactional proxy entirely, so the
 * transaction boundary and optimistic-lock exception translation would
 * silently not apply. Putting it in its own bean means AuctionService's
 * retry loop calls through the proxy on every attempt, as intended.
 */
@Component
@RequiredArgsConstructor
public class BidPlacementExecutor {

    private static final Logger log = LoggerFactory.getLogger(BidPlacementExecutor.class);

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final BidValidationService bidValidationService;
    private final BidBroadcastService bidBroadcastService;
    private final LotServiceClient lotServiceClient;

    @Value("${auction.anti-snipe.window-seconds}")
    private int antiSnipeWindowSeconds;

    @Value("${auction.anti-snipe.extension-minutes}")
    private int antiSnipeExtensionMinutes;

    @Transactional
    public BidResponse execute(Long lotId, Long buyerId, BigDecimal amount) {
        Auction auction = auctionRepository.findByLotId(lotId)
                .orElseThrow(() -> new BidValidationException("No auction found for lot " + lotId));

        bidValidationService.validatePlaceBid(auction, amount);

        boolean withinAntiSnipeWindow =
                bidValidationService.isWithinAntiSnipeWindow(auction, antiSnipeWindowSeconds);

        auction.setCurrentHighestBid(amount);
        auction.setCurrentHighestBidderId(buyerId);

        boolean extended = false;
        if (withinAntiSnipeWindow) {
            auction.setEndTime(Instant.now().plusSeconds(antiSnipeExtensionMinutes * 60L));
            auction.setStatus(Auction.AuctionStatus.EXTENDED);
            extended = true;
        }

        // save() flushes the @Version check for this row - a concurrent
        // update since read will surface as ObjectOptimisticLockingFailureException here.
        auction = auctionRepository.save(auction);

        Bid bid = Bid.builder()
                .auctionId(auction.getId())
                .lotId(lotId)
                .buyerId(buyerId)
                .amount(amount)
                .bidTime(Instant.now())
                .triggeredExtension(extended)
                .build();
        bid = bidRepository.save(bid);

        bidBroadcastService.broadcastNewBid(auction);
        if (extended) {
            log.info("Anti-sniping extension triggered for lot {} - new end time {}",
                    lotId, auction.getEndTime());
            bidBroadcastService.broadcastAuctionExtended(auction);
        }

        syncHighestBidToLotService(lotId, amount, buyerId);

        return new BidResponse(
                bid.getId(),
                lotId,
                buyerId,
                amount,
                bid.getBidTime(),
                auction.getCurrentHighestBid(),
                auction.getEndTime(),
                extended
        );
    }

    /**
     * Best-effort sync of the new highest bid to Lot Service, so its
     * read model (used for lot browsing/search) reflects the current
     * price without buyers having to poll the Bidding Service directly.
     *
     * Deliberately NOT allowed to fail the bid itself: the Bidding
     * Service's own database is the source of truth for auction state.
     * If Lot Service is temporarily unreachable, the bid still stands;
     * the discrepancy self-heals on the next successful bid or at
     * auction close (closeAuction/markSold calls in AuctionClosingService).
     */
    private void syncHighestBidToLotService(Long lotId, BigDecimal amount, Long buyerId) {
        try {
            lotServiceClient.updateHighestBid(lotId, amount, buyerId);
        } catch (Exception ex) {
            log.warn("Failed to sync highest bid to Lot Service for lot {} - will self-heal on next bid/close",
                    lotId, ex);
        }
    }
}
