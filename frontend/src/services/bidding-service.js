import { createApiClient } from './http';

const client = createApiClient(process.env.REACT_APP_BIDDING_URL || 'http://localhost:8083');

// Second call in the "create lot" flow (after lot-service's POST /api/lots) — a lot isn't
// actually biddable/purchasable until an Auction row exists for it here.
export function createAuction({ lotId, farmerId, lotType, startingPrice, fixedPrice, endTime }, token) {
  return client.post('/api/auctions', { lotId, farmerId, lotType, startingPrice, fixedPrice, endTime }, token);
}

export function getAuctionResult(lotId, token) {
  return client.get(`/api/auctions/${lotId}`, token);
}

export function placeBid(lotId, amount, token) {
  return client.post(`/api/auctions/${lotId}/bids`, { amount }, token);
}

export function buyNow(lotId, idempotencyKey, token) {
  return client.post(`/api/fixed-price/${lotId}/buy-now`, { idempotencyKey }, token);
}
