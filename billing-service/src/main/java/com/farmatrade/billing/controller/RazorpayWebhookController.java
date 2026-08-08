package com.farmatrade.billing.controller;

import com.farmatrade.billing.service.RazorpayWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final RazorpayWebhookService webhookService;

    /**
     * Takes the raw request body as a String (not a pre-parsed DTO) because
     * Razorpay's signature is computed over the exact bytes it sent -- a
     * re-serialized copy of a parsed object is not guaranteed to match
     * byte-for-byte, which would make the signature check fail even for
     * genuine requests. Signature verification happens on this raw body
     * before anything is parsed or trusted -- see RazorpayWebhookService.
     */
    @PostMapping("/razorpay")
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false)
            String signature,

            @RequestBody String rawPayload) {

        webhookService.processWebhook(signature, rawPayload);

        return ResponseEntity.ok("Webhook processed");
    }
}
