package com.farmatrade.bidding.repository;

import com.farmatrade.bidding.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByAuctionIdOrderByBidTimeDesc(Long auctionId);

    List<Bid> findByLotIdOrderByBidTimeDesc(Long lotId);

    List<Bid> findByBuyerIdOrderByBidTimeDesc(Long buyerId);
}
