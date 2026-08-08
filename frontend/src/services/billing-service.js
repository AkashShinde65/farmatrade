import { createApiClient } from './http';

const client = createApiClient(process.env.REACT_APP_BILLING_URL || 'http://localhost:8085');

// POST /invoice is not called from here — invoices are created automatically by the backend
// chain (bidding -> logistics -> billing) once a buyer accepts/declines logistics.

export function getInvoicesByBuyer(buyerId, token) {
  return client.get(`/invoice/buyer/${buyerId}`, token);
}

export function getInvoicesByFarmer(farmerId, token) {
  return client.get(`/invoice/farmer/${farmerId}`, token);
}

export function getInvoice(invoiceId, token) {
  return client.get(`/invoice/${invoiceId}`, token);
}

export function getInvoicePdf(invoiceId, token) {
  return client.getBlob(`/invoice/${invoiceId}/pdf`, token);
}

export function createOrder(invoiceId, token) {
  return client.post(`/payment/create-order?invoiceId=${invoiceId}`, undefined, token);
}

// Client-side confirmation path -- delivered by the browser right after Razorpay's checkout
// succeeds, instead of relying on Razorpay's servers reaching this backend via a webhook (which
// needs a public URL/ngrok tunnel that local development doesn't have). The backend still
// verifies razorpaySignature itself before trusting any of this.
export function verifyPayment({ razorpayOrderId, razorpayPaymentId, razorpaySignature }, token) {
  return client.post('/payment/verify', { razorpayOrderId, razorpayPaymentId, razorpaySignature }, token);
}
