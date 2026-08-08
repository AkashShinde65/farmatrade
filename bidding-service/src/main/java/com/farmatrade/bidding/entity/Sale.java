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
 * Represents a completed sale (either auction-won or buy-now). The Bidding
 * Service owns Sale records but NEVER creates invoices - it notifies
 * Logistics Service via LotWonNotification, and Logistics Service (once
 * the buyer's accept/decline choice is known) calls Billing Service, which
 * owns invoice creation/lifecycle.
 */
@Entity
@Table(
        name = "sales",
        indexes = {
                @Index(name = "idx_sale_lot_id", columnList = "lotId"),
                @Index(name = "idx_sale_buyer_id", columnList = "buyerId")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long saleId;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false)
    private Long farmerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Long lotId;

    @Column(nullable = false)
    private Instant soldAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus status;

    @Version
    private Long version;

    public enum SaleStatus {
        /** Sale row persisted, Logistics Service not yet notified. */
        PENDING,
        /** Logistics Service accepted the lot-won notification. */
        NOTIFIED,
        /** Logistics Service call failed after retries - needs manual reconciliation. */
        NOTIFICATION_FAILED
    }
}
