package com.farmatrade.bidding.dto;

import java.math.BigDecimal;

/**
 * Sent to Logistics Service's POST /api/logistics/requests the instant a lot is won (auction
 * close or Buy-Now) -- mirrors Logistics Service's OptInRequest contract field-for-field.
 * Logistics Service (not Bidding Service) calls Billing Service once the buyer's accept/decline
 * choice is known, since only then is logisticsAccepted a real value rather than a placeholder.
 */
public record LotWonNotification(
        Long lotId,
        Long farmerId,
        Long buyerId,
        String farmerAddress,
        Long saleId,
        BigDecimal amount,
        String cropName,
        Double quantity,
        BigDecimal gst,
        BigDecimal platformFee,
        BigDecimal totalAmount,
        String paymentMethod
) {
}
