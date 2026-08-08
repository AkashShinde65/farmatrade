package com.farmatrade.billing.controller;

import com.farmatrade.billing.dto.CreateOrderResponse;
import com.farmatrade.billing.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final RazorpayService razorpayService;

    @PostMapping("/create-order")
    public ResponseEntity<CreateOrderResponse> createOrder(
            @RequestParam Long invoiceId,
            JwtAuthenticationToken authentication) {

        return ResponseEntity.ok(
                razorpayService.createOrder(invoiceId, extractUserId(authentication))
        );
    }

    /**
     * Client-side confirmation path -- see RazorpayService.verifyPayment for why this exists
     * alongside the webhook.
     */
    @PostMapping("/verify")
    public ResponseEntity<Void> verifyPayment(
            @RequestBody VerifyPaymentRequest request,
            JwtAuthenticationToken authentication) {

        razorpayService.verifyPayment(
                extractUserId(authentication),
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature()
        );
        return ResponseEntity.ok().build();
    }

    public record VerifyPaymentRequest(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
    }

    private Long extractUserId(JwtAuthenticationToken authentication) {
        String subject = authentication.getToken().getSubject();
        if (subject == null) {
            throw new IllegalStateException("JWT is missing required 'sub' claim");
        }
        return Long.valueOf(subject);
    }
}
