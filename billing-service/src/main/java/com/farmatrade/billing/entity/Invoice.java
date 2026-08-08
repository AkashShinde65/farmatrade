package com.farmatrade.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceId;

    @Column(nullable = false)
    private Long saleId;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false)
    private Long farmerId;

    @Column(nullable = false)
    private Long lotId;

    @Column(nullable = false)
    private String cropName;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private Double gst;

    @Column(nullable = false)
    private Double platformFee;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private String paymentMethod;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    @Column(nullable = false)
    private Boolean logisticsAccepted;

    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = InvoiceStatus.PENDING;
        }
    }
}
