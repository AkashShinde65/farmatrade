package com.farmatrade.billing.controller;

import com.farmatrade.billing.entity.Invoice;
import com.farmatrade.billing.service.InvoiceService;
import com.farmatrade.billing.service.PdfInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CORRECTED 2026-08-02 -- none of these read endpoints verified the caller actually owned the
 * invoice being requested; any authenticated user could view or download any other buyer's or
 * farmer's invoice just by knowing/guessing its id. Now checks the caller's JWT "sub" against the
 * invoice's real buyerId/farmerId, same pattern already applied to logistics-service's accept/
 * decline and billing-service's own /payment/create-order.
 *
 * Also removed POST /invoice entirely: it had zero callers anywhere in the codebase (the real,
 * correctly-secured invoice-creation path is InternalSaleController's POST /internal/sale, called
 * by logistics-service) and, unlike that one, was reachable by any authenticated user with no
 * ROLE_SERVICE check at all -- meaning any buyer or farmer could have forged an arbitrary invoice
 * for any sale. Dead and insecure, so removed rather than secured.
 */
@RestController
@RequestMapping("/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PdfInvoiceService pdfInvoiceService;

    @GetMapping("/{invoiceId}")
    public ResponseEntity<Invoice> getInvoice(@PathVariable Long invoiceId, JwtAuthenticationToken authentication) {
        Invoice invoice = invoiceService.getInvoice(invoiceId);
        requireOwnership(invoice, extractUserId(authentication));
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<Invoice>> getBuyerInvoices(@PathVariable Long buyerId, JwtAuthenticationToken authentication) {
        requireSelf(buyerId, extractUserId(authentication));
        return ResponseEntity.ok(invoiceService.getBuyerInvoices(buyerId));
    }

    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<List<Invoice>> getFarmerInvoices(@PathVariable Long farmerId, JwtAuthenticationToken authentication) {
        requireSelf(farmerId, extractUserId(authentication));
        return ResponseEntity.ok(invoiceService.getFarmerInvoices(farmerId));
    }

    @GetMapping("/{invoiceId}/pdf")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long invoiceId, JwtAuthenticationToken authentication) {

        Invoice invoice = invoiceService.getInvoice(invoiceId);
        requireOwnership(invoice, extractUserId(authentication));

        byte[] pdf = pdfInvoiceService.generateInvoicePdf(invoiceId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("invoice-" + invoiceId + ".pdf")
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    private void requireOwnership(Invoice invoice, Long callerId) {
        if (!invoice.getBuyerId().equals(callerId) && !invoice.getFarmerId().equals(callerId)) {
            throw new AccessDeniedException(
                    "Invoice " + invoice.getInvoiceId() + " does not belong to the authenticated user");
        }
    }

    private void requireSelf(Long pathId, Long callerId) {
        if (!pathId.equals(callerId)) {
            throw new AccessDeniedException("Cannot view another user's invoices");
        }
    }

    private Long extractUserId(JwtAuthenticationToken authentication) {
        String subject = authentication.getToken().getSubject();
        if (subject == null) {
            throw new IllegalStateException("JWT is missing required 'sub' claim");
        }
        return Long.valueOf(subject);
    }
}
