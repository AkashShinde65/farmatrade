package com.farmatrade.bidding.service;

import com.farmatrade.bidding.dto.LotResponse;
import com.farmatrade.bidding.dto.LotWonNotification;
import com.farmatrade.bidding.entity.Auction;
import com.farmatrade.bidding.entity.Sale;
import com.farmatrade.bidding.repository.AuctionRepository;
import com.farmatrade.bidding.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Single source of truth for "close this auction out": select the
 * winner, persist the Sale, update Lot Service, notify Logistics Service,
 * and broadcast the winner. Used both by AuctionExpiryScheduler (natural
 * expiry) and FixedPriceService (Buy-Now, which closes immediately).
 *
 * Notifies Logistics Service, NOT Billing Service, the instant a sale
 * closes (2026-07-30): the product flow is sale closes -> buyer decides on
 * logistics (cold storage/truck, opt-in) -> invoice is created afterwards,
 * reflecting the real logisticsAccepted outcome. Billing Service's
 * /internal/sale is now called by Logistics Service itself once that
 * decision is made (accept or decline) - see its own BillingServiceClient.
 */
@Service
@RequiredArgsConstructor
public class AuctionClosingService {

    private static final Logger log = LoggerFactory.getLogger(AuctionClosingService.class);
    private static final String UNSPECIFIED_PAYMENT_METHOD = "UNSPECIFIED";

    private final AuctionRepository auctionRepository;
    private final SaleRepository saleRepository;
    private final LotServiceClient lotServiceClient;
    private final LogisticsServiceClient logisticsServiceClient;
    private final BidBroadcastService bidBroadcastService;

    /**
     * UNCONFIRMED placeholder rates - Billing/Product need to confirm the
     * real GST and platform-fee calculation before this is trustworthy for
     * real invoices. Computed here (not in Logistics Service) since Bidding
     * Service already owns the sale amount and these config values.
     */
    @Value("${billing.gst-rate:0.05}")
    private BigDecimal gstRate;

    @Value("${billing.platform-fee-rate:0.02}")
    private BigDecimal platformFeeRate;

    /**
     * Closes an auction that reached its natural end time via the highest
     * bid received. No-op (besides marking CLOSED) if there were no bids.
     *
     * CORRECTED 2026-08-01 -- the status mutations below were never actually persisted:
     * AuctionExpiryScheduler loads expired auctions via a plain (non-transactional) repository
     * query, whose own short-lived transaction closes before this @Transactional method even
     * starts. By the time auction.setStatus(...) runs here, the entity is detached from this
     * method's own persistence context, so the change only ever existed in memory. Confirmed live
     * in the database: an expired auction stayed status=ACTIVE indefinitely, so the scheduler kept
     * re-finding and re-closing it every 5 seconds, generating a new duplicate Sale row (and a new
     * failed logistics notification) each time -- 131 duplicates for one lot in under 20 minutes
     * before this was caught. Fixed by explicitly re-saving the auction after mutating it.
     */
    @Transactional
    public void closeExpiredAuction(Auction auction) {
        if (auction.getCurrentHighestBidderId() == null) {
            auction.setStatus(Auction.AuctionStatus.CLOSED);
            auctionRepository.save(auction);
            log.info("Auction for lot {} closed with no bids", auction.getLotId());
            bidBroadcastService.broadcastAuctionClosed(auction);
            lotServiceClient.closeAuction(auction.getLotId());
            return;
        }

        finalizeSale(auction, auction.getCurrentHighestBidderId(), auction.getCurrentHighestBid());
    }

    /**
     * Closes a FIXED_PRICE auction immediately as a result of a Buy-Now
     * purchase.
     */
    @Transactional
    public void closeBuyNow(Auction auction, Long buyerId, BigDecimal amount) {
        finalizeSale(auction, buyerId, amount);
    }

    private void finalizeSale(Auction auction, Long winnerId, BigDecimal winningAmount) {
        auction.setStatus(Auction.AuctionStatus.SOLD);
        auction.setCurrentHighestBidderId(winnerId);
        auction.setCurrentHighestBid(winningAmount);
        auctionRepository.save(auction);

        Sale sale = Sale.builder()
                .buyerId(winnerId)
                .farmerId(auction.getFarmerId())
                .amount(winningAmount)
                .lotId(auction.getLotId())
                .soldAt(Instant.now())
                .status(Sale.SaleStatus.PENDING)
                .build();
        sale = saleRepository.save(sale);

        log.info("Sale {} generated for lot {} - winner={}, amount={}",
                sale.getSaleId(), auction.getLotId(), winnerId, winningAmount);

        lotServiceClient.markSold(auction.getLotId(), winnerId, winningAmount);
        lotServiceClient.closeAuction(auction.getLotId());

        LotWonNotification notification = buildLotWonNotification(sale);

        boolean notified = logisticsServiceClient.notifyLotWon(notification);
        sale.setStatus(notified ? Sale.SaleStatus.NOTIFIED : Sale.SaleStatus.NOTIFICATION_FAILED);
        saleRepository.save(sale);

        if (!notified) {
            log.error("Logistics notification FAILED for saleId={} - flagged for manual reconciliation", sale.getSaleId());
        }

        bidBroadcastService.broadcastAuctionClosed(auction);
        bidBroadcastService.broadcastWinner(auction.getLotId(), winnerId, winningAmount);
    }

    /**
     * Builds Logistics Service's LotWonNotification. gst/platformFee/totalAmount are computed
     * here from placeholder rates (see class Javadoc) and carried through Logistics Service
     * unchanged, since Billing Service's /internal/sale contract needs them but only Logistics
     * Service knows the real logisticsAccepted outcome to send alongside them.
     */
    private LotWonNotification buildLotWonNotification(Sale sale) {
        LotResponse lot = fetchLotDetailsBestEffort(sale.getLotId());

        BigDecimal gst = sale.getAmount().multiply(gstRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFee = sale.getAmount().multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = sale.getAmount().add(gst).add(platformFee).setScale(2, RoundingMode.HALF_UP);

        return new LotWonNotification(
                sale.getLotId(),
                sale.getFarmerId(),
                sale.getBuyerId(),
                lot != null ? lot.locationName() : null,
                sale.getSaleId(),
                sale.getAmount(),
                lot != null ? lot.cropName() : "UNKNOWN",
                lot != null && lot.quantity() != null ? lot.quantity().doubleValue() : null,
                gst,
                platformFee,
                totalAmount,
                UNSPECIFIED_PAYMENT_METHOD
        );
    }

    /**
     * Crop name/quantity/location are supplementary metadata for this notification, not the
     * source of truth for the sale itself (amount/buyer/farmer already come from Bidding
     * Service's own Sale record). If Lot Service is unreachable, log and fall back rather than
     * blocking an already-decided sale on a metadata lookup - though a missing farmerAddress
     * will predictably fail Logistics Service's validation and surface as NOTIFICATION_FAILED.
     */
    private LotResponse fetchLotDetailsBestEffort(Long lotId) {
        try {
            return lotServiceClient.getLot(lotId);
        } catch (Exception ex) {
            log.warn("Could not fetch lot details for {} while building logistics notification - "
                    + "sending cropName=UNKNOWN, quantity=null, locationName=null", lotId, ex);
            return null;
        }
    }
}
