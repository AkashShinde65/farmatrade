package com.farmatrade.bidding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the FarmaTrade Bidding Service (P3).
 *
 * Responsibilities of this microservice:
 *  - Live auction lifecycle (create, extend, close)
 *  - Bid placement and validation
 *  - Buy-Now (fixed price) purchases
 *  - Real-time broadcasting over WebSocket/STOMP using Spring's built-in
 *    in-memory simple message broker (no Redis/Kafka/RabbitMQ)
 *  - Winner selection and Sale generation
 *  - Integration with Lot Service and Billing Service via REST/WebClient
 *
 * {@code @EnableScheduling} is required for the auction closing / anti-sniping
 * scheduler that periodically checks for expired auctions.
 */
@SpringBootApplication
@EnableScheduling
public class BiddingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiddingServiceApplication.class, args);
    }
}
