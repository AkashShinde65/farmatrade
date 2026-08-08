package com.farmatrade.lot.entity;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.farmatrade.lot.enums.LotStatus;
import com.farmatrade.lot.enums.SaleType;
import com.farmatrade.lot.enums.Unit;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "lots")
@Data
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long farmerId;

    private String cropName;

    private String grade;

    private double quantity;

    @Enumerated(EnumType.STRING)
    private Unit unit;

    @Enumerated(EnumType.STRING)
    private SaleType saleType;

    private BigDecimal basePrice;

    private BigDecimal fixedPrice; 

    private Integer auctionDurationMinutes;

    @Enumerated(EnumType.STRING)
    private LotStatus status;

    private BigDecimal currentHighestBid;

    private Long currentHighestBidderId;
    
    private Long winningBuyerId;

    //location
    private Double latitude;

    private Double longitude;

    private String locationName;
    
    @Column(length = 500)
    private String imageUrl;
    
    private String imagePublicId;

    private LocalDateTime auctionEndsAt;
    
    private BigDecimal mandiReferencePrice;

    // Weather captured once, the day the lot is listed -- the same 3-day snapshot (today + 2
    // more days) is then shown on every subsequent view (list/search/get-by-id), not re-fetched
    // fresh each time. forecast is the only genuinely nested part (3 daily entries), stored as a
    // JSON string; everything else stays a plain queryable column.
    private Double weatherTempC;

    private Double weatherHumidityPct;

    private String weatherRiskLevel;

    @Column(columnDefinition = "TEXT")
    private String weatherForecastJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
