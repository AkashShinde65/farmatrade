import { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const BIDDING_URL = process.env.REACT_APP_BIDDING_URL || 'http://localhost:8083';

// Subscribes to /topic/auction/{lotId} for live NEW_BID / AUCTION_EXTENDED / AUCTION_CLOSED /
// WINNER_ANNOUNCED events. Returns only the most recent event — callers that need bid history
// build it up themselves from successive values.
export function useAuctionSocket(lotId) {
  const [lastEvent, setLastEvent] = useState(null);

  useEffect(() => {
    if (!lotId) return undefined;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${BIDDING_URL}/ws`),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/auction/${lotId}`, (message) => {
          try {
            setLastEvent(JSON.parse(message.body));
          } catch {
            // ignore malformed message
          }
        });
      },
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [lotId]);

  return lastEvent;
}
