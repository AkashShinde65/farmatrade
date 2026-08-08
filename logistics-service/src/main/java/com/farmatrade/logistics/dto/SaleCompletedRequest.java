package com.farmatrade.logistics.dto;

import java.math.BigDecimal;

/**
 * Mirrors Billing Service's POST /internal/sale contract exactly (see
 * billing-service's own SaleCompletedRequest). Logistics Service owns
 * calling this endpoint, since it's the only service that knows the real
 * logisticsAccepted outcome once the buyer has accepted or declined.
 */
public record SaleCompletedRequest(
        Long saleId,
        Long buyerId,
        Long farmerId,
        Long lotId,
        String cropName,
        Double quantity,
        BigDecimal amount,
        BigDecimal gst,
        BigDecimal platformFee,
        BigDecimal totalAmount,
        String paymentMethod,
        Boolean logisticsAccepted
) {
}
