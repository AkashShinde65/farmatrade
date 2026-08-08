package com.farmatrade.bidding.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single bid placed by a buyer on an auction. Bids are append-only /
 * immutable historical records - they are never updated after insert,
 * which is why no @Version is needed here (the concurrency-sensitive
 * mutation happens on the Auction row, not on individual Bid rows).
 */
@Entity
@Table(
        name = "bids",
        indexes = {
                @Index(name = "idx_bid_auction_id", columnList = "auctionId"),
                @Index(name = "idx_bid_buyer_id", columnList = "buyerId")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long auctionId;

    @Column(nullable = false)
    private Long lotId;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant bidTime;

    /** True if this bid triggered an anti-sniping extension. */
    @Column(nullable = false)
    private boolean triggeredExtension;
}
