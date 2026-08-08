package com.farmatrade.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payout_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ledgerId;

    @Column(nullable = false)
    private Long farmerId;

    @Column(nullable = false)
    private Long invoiceId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime settledAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}