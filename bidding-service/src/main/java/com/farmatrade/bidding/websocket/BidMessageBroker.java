package com.farmatrade.bidding.websocket;

import com.farmatrade.bidding.dto.BidBroadcastResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around SimpMessagingTemplate that owns the STOMP topic
 * naming convention for auction events. Kept separate from
 * BidBroadcastService so the "how do I send a message" concern is
 * isolated from the "what event happened, what should the payload
 * contain" business concern.
 *
 * Topic convention: /topic/auction/{lotId}
 */
@Component
@RequiredArgsConstructor
public class BidMessageBroker {

    private static final Logger log = LoggerFactory.getLogger(BidMessageBroker.class);
    private static final String TOPIC_PREFIX = "/topic/auction/";

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(BidBroadcastResponse event) {
        String destination = TOPIC_PREFIX + event.lotId();
        log.debug("Broadcasting {} on {}", event.eventType(), destination);
        messagingTemplate.convertAndSend(destination, event);
    }
}
