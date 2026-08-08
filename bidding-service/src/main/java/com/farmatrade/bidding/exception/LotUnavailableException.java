package com.farmatrade.bidding.exception;

/**
 * Thrown when Lot Service reports that a lot does not exist, is not
 * available for bidding, or cannot be reached. Maps to HTTP 424 Failed
 * Dependency / 404, depending on the underlying cause.
 */
public class LotUnavailableException extends RuntimeException {

    public LotUnavailableException(String message) {
        super(message);
    }

    public LotUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
