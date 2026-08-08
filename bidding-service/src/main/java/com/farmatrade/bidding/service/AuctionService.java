package com.farmatrade.bidding.service;

import com.farmatrade.bidding.dto.AuctionResultResponse;
import com.farmatrade.bidding.dto.BidResponse;
import com.farmatrade.bidding.entity.Auction;
import com.farmatrade.bidding.entity.Sale;
import com.farmatrade.bidding.exception.BidValidationException;
import com.farmatrade.bidding.repository.AuctionRepository;
import com.farmatrade.bidding.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Orchestrates the core live-auction workflow: creating auctions and
 * placing bids (including anti-sniping extension and optimistic-lock
 * conflict retries, the latter delegated per-attempt to
 * BidPlacementExecutor so the transaction boundary is honored correctly).
 */
@Service
@RequiredArgsConstructor
public class AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);
    private static final int MAX_OPTIMISTIC_LOCK_RETRIES = 3;

    private final AuctionRepository auctionRepository;
    private final SaleRepository saleRepository;
    private final BidPlacementExecutor bidPlacementExecutor;

    @Transactional
    public Auction createAuction(Long lotId, Long farmerId, Auction.LotType lotType,
                                  BigDecimal startingPrice, BigDecimal fixedPrice, Instant endTime) {

        if (auctionRepository.findByLotId(lotId).isPresent()) {
            throw new BidValidationException("An auction already exists for lot " + lotId);
        }

        Auction auction = Auction.builder()
                .lotId(lotId)
                .farmerId(farmerId)
                .lotType(lotType)
                .status(Auction.AuctionStatus.ACTIVE)
                .startingPrice(startingPrice)
                .fixedPrice(fixedPrice)
                .endTime(endTime)
                .createdAt(Instant.now())
                .build();

        auction = auctionRepository.save(auction);
        log.info("Auction created for lot {} (type={}, endTime={})", lotId, lotType, endTime);
        return auction;
    }

    /**
     * Places a bid on behalf of buyerId. Retries a bounded number of times
     * on optimistic-lock conflicts, since concurrent bids on a hot auction
     * are the expected case, not an exceptional one. Each attempt runs in
     * its own fresh transaction via BidPlacementExecutor.
     */
    public BidResponse placeBid(Long lotId, Long buyerId, BigDecimal amount) {
        for (int attempt = 1; attempt <= MAX_OPTIMISTIC_LOCK_RETRIES; attempt++) {
            try {
                return bidPlacementExecutor.execute(lotId, buyerId, amount);
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.warn("Optimistic lock conflict placing bid on lot {} (attempt {}/{})",
                        lotId, attempt, MAX_OPTIMISTIC_LOCK_RETRIES);
                if (attempt == MAX_OPTIMISTIC_LOCK_RETRIES) {
                    throw new BidValidationException(
                            "Too many concurrent bids on lot " + lotId + " - please retry");
                }
            }
        }
        // Unreachable - loop above always returns or throws.
        throw new BidValidationException("Unable to place bid on lot " + lotId);
    }

    @Transactional(readOnly = true)
    public AuctionResultResponse getAuctionResult(Long lotId) {
        Auction auction = auctionRepository.findByLotId(lotId)
                .orElseThrow(() -> new BidValidationException("No auction found for lot " + lotId));

        Long saleId = null;
        if (auction.getStatus() == Auction.AuctionStatus.SOLD) {
            saleId = saleRepository.findByLotId(lotId).map(Sale::getSaleId).orElse(null);
        }

        boolean isFinal = auction.getStatus() == Auction.AuctionStatus.SOLD
                || auction.getStatus() == Auction.AuctionStatus.CLOSED;

        return new AuctionResultResponse(
                auction.getLotId(),
                auction.getStatus(),
                auction.getCurrentHighestBidderId(),
                auction.getCurrentHighestBid(),
                auction.getEndTime(),
                isFinal ? Instant.now() : null,
                saleId
        );
    }
}
