package com.farmatrade.bidding.service;

import com.farmatrade.bidding.dto.LotWonNotification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Notifies Logistics Service the instant a lot is won, so the buyer can decide whether to opt
 * into cold storage + truck logistics. Logistics Service -- not Bidding Service -- is the one
 * that calls Billing Service afterwards, once that decision is known (see its own
 * BillingServiceClient / LogisticsRequestService).
 *
 * Failure handling: 3 retries with exponential backoff for transient errors; on final failure
 * the caller (AuctionClosingService) marks the Sale row as NOTIFICATION_FAILED for manual
 * reconciliation/ops alerting rather than blocking auction closure indefinitely.
 */
@Service
@RequiredArgsConstructor
public class LogisticsServiceClient {

    private static final Logger log = LoggerFactory.getLogger(LogisticsServiceClient.class);
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(500);

    private final WebClient logisticsServiceWebClient;

    /**
     * @return true if Logistics Service accepted the lot-won notification, false otherwise.
     */
    public boolean notifyLotWon(LotWonNotification notification) {
        try {
            logisticsServiceWebClient.post()
                    .uri("/api/logistics/requests")
                    .bodyValue(notification)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_BACKOFF))
                    .block();
            return true;
        } catch (Exception ex) {
            log.error("Logistics Service notification failed for saleId={} lotId={}",
                    notification.saleId(), notification.lotId(), ex);
            return false;
        }
    }
}
