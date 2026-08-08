package com.farmatrade.bidding.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for placing a bid on an active auction.
 *
 * buyerId is deliberately NOT part of this DTO - it is resolved from the
 * authenticated JWT principal in the controller/service layer so a buyer
 * can never place a bid on behalf of someone else by tampering with the
 * request body.
 */
public record PlaceBidRequest(

        @NotNull(message = "Bid amount is required")
        @DecimalMin(value = "0.01", message = "Bid amount must be greater than zero")
        BigDecimal amount

) {
}
