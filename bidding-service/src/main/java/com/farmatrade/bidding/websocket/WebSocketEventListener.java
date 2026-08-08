package com.farmatrade.bidding.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * Observability hook for the WebSocket/STOMP layer. Logs connect,
 * subscribe, and disconnect events so operators can see how many
 * clients are watching live auctions and diagnose connection churn.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        log.info("WebSocket session connected: sessionId={}", event.getMessage().getHeaders().get("simpSessionId"));
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        log.debug("WebSocket subscription: sessionId={}, destination={}",
                event.getMessage().getHeaders().get("simpSessionId"),
                event.getMessage().getHeaders().get("simpDestination"));
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        log.info("WebSocket session disconnected: sessionId={}, closeStatus={}",
                event.getSessionId(), event.getCloseStatus());
    }
}
