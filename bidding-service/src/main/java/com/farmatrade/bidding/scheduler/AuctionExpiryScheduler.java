package com.farmatrade.bidding.scheduler;

import com.farmatrade.bidding.entity.Auction;
import com.farmatrade.bidding.repository.AuctionRepository;
import com.farmatrade.bidding.service.AuctionClosingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Periodically scans for ACTIVE/EXTENDED auctions whose end time has
 * passed and closes them: selects the highest bidder as winner (or marks
 * CLOSED with no sale if there were no bids), generates the Sale,
 * notifies Billing Service, updates Lot Service, and broadcasts the
 * winner - all via AuctionClosingService.
 *
 * Runs every 5 seconds. This interval is a deliberate tradeoff: tight
 * enough that "auction closes automatically" feels immediate to users,
 * loose enough not to hammer the database on every tick. Each auction is
 * closed inside its own transaction (see AuctionClosingService) so one
 * failure doesn't roll back the closing of other expired auctions in the
 * same scheduler run.
 */
@Component
@RequiredArgsConstructor
public class AuctionExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuctionExpiryScheduler.class);

    private final AuctionRepository auctionRepository;
    private final AuctionClosingService auctionClosingService;

    @Scheduled(fixedRateString = "${auction.scheduler.fixed-rate-ms:5000}")
    public void closeExpiredAuctions() {
        List<Auction> expired = auctionRepository.findByStatusInAndEndTimeLessThanEqual(
                List.of(Auction.AuctionStatus.ACTIVE, Auction.AuctionStatus.EXTENDED),
                Instant.now()
        );

        if (expired.isEmpty()) {
            return;
        }

        log.info("Found {} expired auction(s) to close", expired.size());

        for (Auction auction : expired) {
            try {
                auctionClosingService.closeExpiredAuction(auction);
            } catch (Exception ex) {
                // Isolate failures per-auction so one bad row doesn't stop
                // the rest of the batch from closing.
                log.error("Failed to close auction for lot {}", auction.getLotId(), ex);
            }
        }
    }
}
