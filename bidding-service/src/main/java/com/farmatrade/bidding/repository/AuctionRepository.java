package com.farmatrade.bidding.repository;

import com.farmatrade.bidding.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    Optional<Auction> findByLotId(Long lotId);

    /** Used by AuctionExpiryScheduler to find auctions that have expired. */
    List<Auction> findByStatusInAndEndTimeLessThanEqual(
            List<Auction.AuctionStatus> statuses, Instant now);
}
