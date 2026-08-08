package com.farmatrade.billing.service;

import com.farmatrade.billing.dto.RazorpayWebhookPayload;
import com.farmatrade.billing.entity.Invoice;
import com.farmatrade.billing.entity.InvoiceStatus;
import com.farmatrade.billing.exception.InvalidWebhookSignatureException;
import com.farmatrade.billing.repository.InvoiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RazorpayWebhookService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookService.class);

    private final InvoiceRepository invoiceRepository;
    private final PayoutLedgerService payoutLedgerService;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    /**
     * Verifies the payload against Razorpay's signature BEFORE trusting anything in it.
     * Previously this method received an already-deserialized payload and never checked the
     * signature parameter at all -- meaning anyone who knew a valid razorpayOrderId could POST a
     * fabricated payload and mark that invoice paid for free. Verification must happen on the raw
     * request body string, since the signature is computed over those exact bytes.
     */
    public void processWebhook(String signature, String rawPayload) {
        if (signature == null || signature.isBlank()) {
            throw new InvalidWebhookSignatureException("Missing X-Razorpay-Signature header.");
        }

        boolean verified;
        try {
            verified = Utils.verifyWebhookSignature(rawPayload, signature, webhookSecret);
        } catch (Exception e) {
            log.error("Error verifying Razorpay webhook signature", e);
            throw new InvalidWebhookSignatureException("Unable to verify webhook signature.");
        }

        if (!verified) {
            throw new InvalidWebhookSignatureException("Webhook signature does not match payload.");
        }

        RazorpayWebhookPayload payload = parsePayload(rawPayload);

        if (payload == null
                || payload.getPayload() == null
                || payload.getPayload().getPayment() == null
                || payload.getPayload().getPayment().getEntity() == null) {
            return;
        }

        String orderId = payload.getPayload().getPayment().getEntity().getOrder_id();
        String paymentId = payload.getPayload().getPayment().getEntity().getId();

        Invoice invoice = invoiceRepository
                .findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setRazorpayPaymentId(paymentId);
        invoice.setPaidAt(LocalDateTime.now());

        invoiceRepository.save(invoice);

        payoutLedgerService.createLedgerEntry(invoice);
    }

    private RazorpayWebhookPayload parsePayload(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, RazorpayWebhookPayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed webhook payload.", e);
        }
    }
}
