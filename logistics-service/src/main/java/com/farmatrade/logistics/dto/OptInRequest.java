package com.farmatrade.logistics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Sent by bidding-service the instant a lot is won (auction close or Buy-Now) -- see
 * LogisticsRequestController.createFromLotWin. Carries everything Billing Service's
 * /internal/sale contract needs (see BillingServiceClient / SaleCompletedRequest), since
 * Logistics Service -- not Bidding Service -- is the one that calls Billing Service, once the
 * buyer's accept/decline choice is known and logisticsAccepted can be a real value rather than a
 * sale-close-time placeholder.
 */
public record OptInRequest(
        @NotNull Long lotId,
        @NotNull Long farmerId,
        @NotNull Long buyerId,
        @NotBlank String farmerAddress,
        @NotNull Long saleId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String cropName,
        Double quantity,
        @NotNull BigDecimal gst,
        @NotNull BigDecimal platformFee,
        @NotNull BigDecimal totalAmount,
        @NotBlank String paymentMethod
) {
}
