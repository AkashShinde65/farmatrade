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
 * Local read/write model of an auction owned by the Bidding Service.
 *
 * NOTE: this class is not literally named in the requested package
 * listing, but it is the supporting entity required to implement
 * "Auction Status = ACTIVE", "Auction End Time", anti-sniping extension,
 * and the closing scheduler without round-tripping to Lot Service on
 * every single bid. Per the coding standards ("generate every required
 * supporting class automatically"), it is generated here.
 *
 * The Bidding Service NEVER owns lot inventory data (title, description,
 * farmer profile, etc.) - that stays in Lot Service. This entity only
 * mirrors the minimal auction-state fields the Bidding Service needs to
 * enforce bidding rules locally, and is kept in sync via LotServiceClient.
 */
@Entity
@Table(
        name = "auctions",
        indexes = {
                @Index(name = "idx_auction_lot_id", columnList = "lotId", unique = true),
                @Index(name = "idx_auction_status_end_time", columnList = "status,endTime")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long lotId;

    @Column(nullable = false)
    private Long farmerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LotType lotType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal startingPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal currentHighestBid;

    private Long currentHighestBidderId;

    /** Only populated when lotType = FIXED_PRICE. */
    @Column(precision = 12, scale = 2)
    private BigDecimal fixedPrice;

    @Column(nullable = false)
    private Instant endTime;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Optimistic locking token. Guards the "read current highest bid,
     * compare, write new highest bid" critical section against lost
     * updates when two bids race on the same auction.
     */
    @Version
    private Long version;

    public enum LotType {
        AUCTION,
        FIXED_PRICE
    }

    public enum AuctionStatus {
        ACTIVE,
        EXTENDED,
        CLOSED,
        SOLD,
        CANCELLED
    }
}
