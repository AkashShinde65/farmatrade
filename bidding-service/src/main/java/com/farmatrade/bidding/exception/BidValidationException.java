package com.farmatrade.bidding.exception;

/**
 * Thrown when a bid fails business validation (amount too low, auction
 * not active, buyer not eligible, etc). Maps to HTTP 400 Bad Request.
 */
public class BidValidationException extends RuntimeException {

    public BidValidationException(String message) {
        super(message);
    }
}
