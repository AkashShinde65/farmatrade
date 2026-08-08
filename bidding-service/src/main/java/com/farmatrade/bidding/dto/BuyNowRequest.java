package com.farmatrade.bidding.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for a Buy-Now purchase on a FIXED_PRICE lot.
 *
 * idempotencyKey lets a client safely retry a buy-now call (e.g. after a
 * network timeout) without risking a double purchase; the service treats
 * a repeated key on an already-SOLD lot as a no-op success rather than an
 * error.
 */
public record BuyNowRequest(

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey

) {
}
