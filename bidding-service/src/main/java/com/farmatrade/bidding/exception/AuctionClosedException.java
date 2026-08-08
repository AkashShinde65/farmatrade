package com.farmatrade.bidding.exception;

/**
 * Thrown when a bid or buy-now attempt targets an auction that has
 * already closed/sold/been cancelled. Maps to HTTP 409 Conflict.
 */
public class AuctionClosedException extends RuntimeException {

    public AuctionClosedException(String message) {
        super(message);
    }
}
