package com.farmatrade.bidding.dto;

import java.time.Instant;

/**
 * Standard error envelope returned for every 4xx/5xx response so API
 * consumers (frontend, Postman, other services) get a consistent shape.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId
) {
}
