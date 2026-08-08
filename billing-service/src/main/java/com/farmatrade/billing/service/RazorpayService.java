package com.farmatrade.billing.service;

import com.farmatrade.billing.dto.CreateOrderResponse;
import com.farmatrade.billing.entity.Invoice;
import com.farmatrade.billing.entity.InvoiceStatus;
import com.farmatrade.billing.exception.InvalidWebhookSignatureException;
import com.farmatrade.billing.repository.InvoiceRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RazorpayService {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayService.class);

    private final InvoiceRepository invoiceRepository;
    private final PayoutLedgerService payoutLedgerService;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    private RazorpayClient getClient() {
        try {
            return new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Previously trusted invoiceId alone -- any authenticated buyer could create a Razorpay
     * payment order for someone else's invoice just by guessing/knowing its id. callerId comes
     * from the caller's own JWT "sub" claim, never from the request itself.
     */
    public CreateOrderResponse createOrder(Long invoiceId, Long callerId) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if (!invoice.getBuyerId().equals(callerId)) {
            throw new AccessDeniedException("Invoice " + invoiceId + " does not belong to the authenticated user");
        }

        try {

            JSONObject request = new JSONObject();

            // CORRECTED 2026-08-02 -- this charged invoice.getAmount() (the base sale price
            // before GST/platform fee), silently undercharging every buyer by that difference.
            // Found live while testing the real Razorpay checkout end-to-end. totalAmount is
            // what the invoice actually says the buyer owes.
            request.put("amount", Math.round(invoice.getTotalAmount() * 100));
            request.put("currency", "INR");
            request.put("receipt", invoice.getInvoiceNumber());

            Order order = getClient().orders.create(request);

            invoice.setRazorpayOrderId(order.get("id"));
            invoiceRepository.save(invoice);

            return CreateOrderResponse.builder()
                    .orderId(order.get("id"))
                    .amount(invoice.getTotalAmount())
                    .currency(order.get("currency"))
                    .keyId(keyId)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to create Razorpay Order", e);
        }
    }

    /**
     * Added 2026-08-02 -- client-side confirmation path, an alternative to the webhook for local
     * development where Razorpay's servers can't reach this backend (no public URL/ngrok tunnel).
     * When checkout succeeds, Razorpay's widget hands the BROWSER razorpay_order_id/payment_id/
     * signature directly -- the browser already has a real connection to us, so it can deliver
     * this confirmation itself instead of relying on Razorpay's servers reaching us. The
     * signature is still verified server-side with our key secret before trusting any of it, the
     * same trust model as the webhook, just triggered by the frontend instead of Razorpay.
     * Idempotent: a second call for an already-PAID invoice (e.g. webhook arrived first, if one
     * ever does) is a safe no-op rather than double-creating a ledger entry.
     */
    public void verifyPayment(Long callerId, String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {

        Invoice invoice = invoiceRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Invoice not found for order " + razorpayOrderId));

        if (!invoice.getBuyerId().equals(callerId)) {
            throw new AccessDeniedException("Invoice does not belong to the authenticated user");
        }

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return;
        }

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", razorpayOrderId);
        options.put("razorpay_payment_id", razorpayPaymentId);
        options.put("razorpay_signature", razorpaySignature);

        try {
            boolean verified = Utils.verifyPaymentSignature(options, keySecret);
            if (!verified) {
                throw new InvalidWebhookSignatureException("Payment signature does not match.");
            }
        } catch (InvalidWebhookSignatureException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error verifying Razorpay payment signature", e);
            throw new InvalidWebhookSignatureException("Unable to verify payment signature.");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setRazorpayPaymentId(razorpayPaymentId);
        invoice.setPaidAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        payoutLedgerService.createLedgerEntry(invoice);
    }
}
