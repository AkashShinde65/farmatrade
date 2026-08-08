package com.farmatrade.bidding.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

/**
 * WebClient beans used to call downstream microservices (Lot Service,
 * Logistics Service). Each gets its own bean, base URL, and connect/read
 * timeout so a slow Logistics Service can't stall Lot Service calls or
 * vice versa.
 *
 * IMPORTANT - Authorization header:
 * every outbound call was previously sent with NO credentials at all,
 * so LotServiceClient/LogisticsServiceClient would get 401'd by the
 * receiving service before the payload even mattered. This config
 * attaches "Authorization: Bearer {internal-service.token}" to every
 * request made by either WebClient via internalServiceAuthFilter().
 *
 * This is a STOP-GAP, not a real service-credential flow: there is no
 * client-credentials grant from auth-service, no mTLS, nothing. A single
 * static shared-secret token read from an environment variable is the
 * minimum viable thing that unblocks Lot/Logistics calls made both from
 * an HTTP request (a buyer placing a bid) AND from AuctionExpiryScheduler
 * (a background job with no inbound user JWT to forward) - which is why
 * simply forwarding the caller's own bearer token would NOT have worked
 * for the scheduler path.
 *
 * Resolved (2026-07-30): Lot Service, Billing Service, and Logistics
 * Service each run an InternalServiceTokenFilter ahead of their normal
 * OAuth2 resource server filter - if the Authorization header exactly
 * matches this same static token, the caller is granted ROLE_SERVICE
 * without needing a real JWT (see InternalAuctionController's own
 * "/internal/**" matcher for the same pattern, applied here to the
 * receiving side). INTERNAL_SERVICE_TOKEN must be set to the same value
 * across all four services - still a shared secret, not short-lived
 * per-request credentials, so revisit if that stronger guarantee is
 * ever needed.
 */
@Configuration
public class WebClientConfig {

    @Value("${lot-service.base-url}")
    private String lotServiceBaseUrl;

    @Value("${logistics-service.base-url}")
    private String logisticsServiceBaseUrl;

    @Value("${webclient.timeout.connect-ms}")
    private int connectTimeoutMs;

    @Value("${webclient.timeout.response-ms}")
    private int responseTimeoutMs;

    @Value("${internal-service.token}")
    private String internalServiceToken;

    @Bean
    public WebClient lotServiceWebClient() {
        return WebClient.builder()
                .baseUrl(lotServiceBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient()))
                .filter(internalServiceAuthFilter())
                .build();
    }

    @Bean
    public WebClient logisticsServiceWebClient() {
        return WebClient.builder()
                .baseUrl(logisticsServiceBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient()))
                .filter(internalServiceAuthFilter())
                .build();
    }

    /**
     * Attaches the interim static service token to every outbound request
     * made through either WebClient. See class-level Javadoc for why this
     * exists and what needs to replace it.
     */
    private ExchangeFilterFunction internalServiceAuthFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request ->
                Mono.just(ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalServiceToken)
                        .build()));
    }

    private HttpClient httpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(java.time.Duration.ofMillis(responseTimeoutMs))
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler(responseTimeoutMs, TimeUnit.MILLISECONDS)));
    }
}
