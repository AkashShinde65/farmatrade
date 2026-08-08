package com.farmatrade.bidding.service;

import com.farmatrade.bidding.dto.LotResponse;
import com.farmatrade.bidding.exception.LotUnavailableException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Consumes Lot Service's REST API only - never touches its database or
 * entity classes. Every downstream call is wrapped with a bounded retry
 * (for transient network/5xx failures) and translated into
 * LotUnavailableException on final failure so callers get a single,
 * well-typed exception to handle.
 *
 * Lots are addressed by Lot Service's own numeric id (Long) - Lot Service
 * has no separate "lot number" business key. getLot() calls the public
 * GET /api/lots/{id}; the write operations call the /internal/lots/{id}/...
 * endpoints added specifically for this service, authenticated via the
 * shared internal-service token (see WebClientConfig).
 */
@Service
@RequiredArgsConstructor
public class LotServiceClient {

    private static final Logger log = LoggerFactory.getLogger(LotServiceClient.class);
    private static final int MAX_RETRIES = 2;
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(300);

    private final WebClient lotServiceWebClient;

    public LotResponse getLot(Long lotId) {
        return lotServiceWebClient.get()
                .uri("/api/lots/{id}", lotId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        Mono.error(new LotUnavailableException("Lot " + lotId + " not found in Lot Service")))
                .bodyToMono(LotResponse.class)
                .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_BACKOFF)
                        .filter(this::isRetryable))
                .onErrorMap(this::mapError)
                .block();
    }

    public void updateHighestBid(Long lotId, BigDecimal amount, Long buyerId) {
        lotServiceWebClient.put()
                .uri("/internal/lots/{id}/highest-bid", lotId)
                .bodyValue(new HighestBidUpdate(lotId, amount, buyerId))
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_BACKOFF).filter(this::isRetryable))
                .onErrorMap(this::mapError)
                .block();
    }

    public void markSold(Long lotId, Long buyerId, BigDecimal amount) {
        lotServiceWebClient.post()
                .uri("/internal/lots/{id}/mark-sold", lotId)
                .bodyValue(new MarkSoldRequest(buyerId, amount))
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_BACKOFF).filter(this::isRetryable))
                .onErrorMap(this::mapError)
                .block();
    }

    public void closeAuction(Long lotId) {
        lotServiceWebClient.post()
                .uri("/internal/lots/{id}/close-auction", lotId)
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_BACKOFF).filter(this::isRetryable))
                .onErrorMap(this::mapError)
                .block();
    }

    private boolean isRetryable(Throwable throwable) {
        return !(throwable instanceof LotUnavailableException);
    }

    private Throwable mapError(Throwable throwable) {
        if (throwable instanceof LotUnavailableException) {
            return throwable;
        }
        log.error("Lot Service call failed", throwable);
        return new LotUnavailableException("Lot Service is unreachable or returned an error", throwable);
    }

    // Mirrors Lot Service's BidUpdateRequest DTO shape (lotId, highestBid, highestBidderId).
    private record HighestBidUpdate(Long lotId, BigDecimal highestBid, Long highestBidderId) {
    }

    // Mirrors Lot Service's MarkSoldRequest DTO shape (buyerId, amount).
    private record MarkSoldRequest(Long buyerId, BigDecimal amount) {
    }
}
