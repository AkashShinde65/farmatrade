package com.farmatrade.logistics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient nominatimRestClient(
            @Value("${logistics.geocoding.nominatim-base-url}") String baseUrl,
            @Value("${logistics.geocoding.user-agent}") String userAgent) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
    }

    @Bean
    public RestClient openMeteoRestClient(
            @Value("${logistics.weather.open-meteo-base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * STOP-GAP service-to-service credential (see bidding-service's WebClientConfig Javadoc for
     * the full rationale) -- there is no client-credentials flow from auth-service yet, so the
     * shared internal-service token is attached as a static bearer credential on every call.
     */
    @Bean
    public RestClient billingServiceRestClient(
            @Value("${billing-service.base-url}") String baseUrl,
            @Value("${internal-service.token}") String internalServiceToken) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + internalServiceToken)
                .build();
    }
}
