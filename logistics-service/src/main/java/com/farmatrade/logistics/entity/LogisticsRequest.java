package com.farmatrade.logistics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "logistics_request")
@Getter
@Setter
@NoArgsConstructor
public class LogisticsRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lot_id", nullable = false)
    private Long lotId;

    @Column(name = "farmer_id", nullable = false)
    private Long farmerId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "farmer_address", nullable = false)
    private String farmerAddress;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LogisticsRequestStatus status = LogisticsRequestStatus.PENDING_CHOICE;

    @Column(name = "truck_id")
    private Long truckId;

    @Enumerated(EnumType.STRING)
    @Column(name = "truck_booking_status", length = 20)
    private TruckBookingStatus truckBookingStatus;

    // Sale details carried through from bidding-service (see OptInRequest) so accept()/decline()
    // can notify Billing Service with the real logisticsAccepted outcome -- see BillingServiceClient.
    // Nullable at the DB level even though OptInRequest requires them for the real flow: some
    // existing tests (e.g. TruckBookingLifecycleIT) persist a bare-bones LogisticsRequest to
    // exercise truck-booking mechanics only, unrelated to billing.
    @Column(name = "sale_id")
    private Long saleId;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "crop_name")
    private String cropName;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "gst", precision = 12, scale = 2)
    private BigDecimal gst;

    @Column(name = "platform_fee", precision = 12, scale = 2)
    private BigDecimal platformFee;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_method")
    private String paymentMethod;

    // Null until accept()/decline() attempts the Billing Service call; true/false after.
    @Column(name = "billing_notified")
    private Boolean billingNotified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
