package com.farmatrade.lot.service;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.farmatrade.lot.dto.MandiPriceApiResponse;
import com.farmatrade.lot.dto.MandiPriceRecord;

@Service
public class MandiPriceClient {

    private static final Logger log =
            LoggerFactory.getLogger(MandiPriceClient.class);

    private final WebClient.Builder webClientBuilder;

    @Value("${mandi.api.base-url}")
    private String baseUrl;

    @Value("${mandi.api.resource-id}")
    private String resourceId;

    @Value("${mandi.api.key}")
    private String apiKey;

    public MandiPriceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public BigDecimal getReferencePrice(
            String cropName,
            String locationName) {

        try {

            log.info("Calling Mandi API for crop: {}, location: {}",
                    cropName, locationName);

            MandiPriceApiResponse response =
                    webClientBuilder
                            .baseUrl(baseUrl)
                            .build()
                            .get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/resource/" + resourceId)
                                    .queryParam("api-key", apiKey)
                                    .queryParam("format", "json")
                                    .queryParam("limit", 50)
                                    .queryParam(
                                            "filters[commodity]",
                                            cropName)
                                    .build())
                            .retrieve()
                            .bodyToMono(MandiPriceApiResponse.class)
                            .block();

            if (response == null ||
                    response.getRecords() == null ||
                    response.getRecords().isEmpty()) {

                log.warn("No mandi price found for crop: {}", cropName);

                return null;
            }

            MandiPriceRecord record =
                    selectBestMatch(response.getRecords(), locationName);

            log.info(
                    "Mandi price found: crop={}, state={}, market={}, modalPrice={}",
                    record.getCommodity(),
                    record.getState(),
                    record.getMarket(),
                    record.getModal_price());

            return record.getModal_price();

        } catch (WebClientResponseException.TooManyRequests e) {

            log.warn("Mandi API rate limit exceeded. HTTP 429.");

            return null;

        } catch (WebClientResponseException e) {

            log.error(
                    "Mandi API error. HTTP status: {}, response: {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString());

            return null;

        } catch (Exception e) {

            log.error(
                    "Unexpected Mandi API error for crop: {}",
                    cropName,
                    e);

            return null;
        }
    }

    /**
     * CORRECTED 2026-08-01 -- locationName was accepted as a parameter but never actually used to
     * filter the government API query, which only ever filtered by commodity. That meant the
     * "reference price" shown could come from any state in the country, not necessarily anywhere
     * near the farmer's actual lot -- found while explaining this feature, not from a bug report.
     *
     * Fixed by requesting more candidate records for the crop in one call (no extra API usage,
     * which matters since this free government API has already been observed rate-limiting us),
     * then picking the first record whose state matches the lot's location client-side. Falls
     * back to the first record overall if no state match is found, so this can only ever be as
     * accurate as before, never return fewer results than today.
     */
    MandiPriceRecord selectBestMatch(List<MandiPriceRecord> records, String locationName) {
        String state = extractState(locationName);

        if (state != null) {
            for (MandiPriceRecord record : records) {
                if (record.getState() != null && record.getState().equalsIgnoreCase(state)) {
                    return record;
                }
            }
            log.warn("No mandi price record matched state '{}' for location '{}' -- "
                    + "falling back to the first available record", state, locationName);
        }

        return records.get(0);
    }

    /**
     * locationName is free-text farmer input, typically "City, State" (e.g. "Ludhiana, Punjab").
     * Takes the last comma-separated segment as the state; returns null if there's no comma to
     * split on, since guessing wrong would be worse than not filtering at all.
     */
    String extractState(String locationName) {
        if (locationName == null || !locationName.contains(",")) {
            return null;
        }
        String[] parts = locationName.split(",");
        String state = parts[parts.length - 1].trim();
        return state.isEmpty() ? null : state;
    }
}