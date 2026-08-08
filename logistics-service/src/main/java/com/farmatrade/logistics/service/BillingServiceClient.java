package com.farmatrade.logistics.service;

import com.farmatrade.logistics.dto.SaleCompletedRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Notifies Billing Service once the buyer's logistics decision (accept/decline) is known -- see
 * LogisticsRequestService.accept()/decline(). This is the single trigger point for invoice
 * creation: Bidding Service intentionally does not call Billing Service directly at sale-close
 * time, since the invoice needs the real logisticsAccepted outcome, not a placeholder.
 *
 * Best-effort: a failure here does not undo the buyer's already-persisted accept/decline choice
 * (see LogisticsRequest.billingNotified for manual-reconciliation tracking).
 */
@Service
public class BillingServiceClient {

    private static final Logger log = LoggerFactory.getLogger(BillingServiceClient.class);

    private final RestClient billingServiceRestClient;

    public BillingServiceClient(RestClient billingServiceRestClient) {
        this.billingServiceRestClient = billingServiceRestClient;
    }

    public boolean notifySaleCompleted(SaleCompletedRequest request) {
        try {
            billingServiceRestClient.post()
                    .uri("/internal/sale")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.error("Billing Service notification failed for saleId={} lotId={}",
                    request.saleId(), request.lotId(), ex);
            return false;
        }
    }
}
