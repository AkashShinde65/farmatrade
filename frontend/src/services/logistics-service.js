import { createApiClient } from './http';

const client = createApiClient(process.env.REACT_APP_LOGISTICS_URL || 'http://localhost:8084');

// POST /api/logistics/requests is bidding-service-internal only — never called from here.

export function getRequestsByBuyer(buyerId, token) {
  return client.get(`/api/logistics/requests?buyerId=${buyerId}`, token);
}

export function getRequestById(id, token) {
  return client.get(`/api/logistics/requests/${id}`, token);
}

export function acceptRequest(id, token) {
  return client.post(`/api/logistics/requests/${id}/accept`, undefined, token);
}

export function declineRequest(id, token) {
  return client.post(`/api/logistics/requests/${id}/decline`, undefined, token);
}

export function getFleet(token) {
  return client.get('/api/logistics/truck-bookings/fleet', token);
}
