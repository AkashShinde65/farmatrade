package com.farmatrade.bidding.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket (with SockJS fallback) configuration.
 *
 * Per project constraints, this uses Spring's built-in in-memory
 * "simple broker" - NOT Redis, NOT an external broker like RabbitMQ.
 *
 * Tradeoff to be aware of: the simple broker only fans out messages to
 * clients connected to THIS JVM instance. If bidding-service is scaled
 * to multiple replicas behind a load balancer, a client connected to
 * instance A will not see a broadcast triggered by a bid processed on
 * instance B. Acceptable for the current single-instance deployment;
 * documented in Best Practices as a future improvement (e.g. sticky
 * sessions at the LB, or revisiting a shared broker once/if allowed).
 *
 * Destinations:
 *  - Clients connect to:      /ws  (SockJS fallback enabled)
 *  - Server broadcasts on:    /topic/auction/{lotId}
 *  - Clients send commands to (if any) prefixed with: /app
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:*", "https://*.farmatrade.local")
                .withSockJS();
    }
}
