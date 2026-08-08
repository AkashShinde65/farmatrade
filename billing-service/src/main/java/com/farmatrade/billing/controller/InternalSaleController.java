package com.farmatrade.billing.controller;

import com.farmatrade.billing.dto.SaleCompletedRequest;
import com.farmatrade.billing.entity.Invoice;
import com.farmatrade.billing.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalSaleController {

    private final InvoiceService invoiceService;

    @PostMapping("/sale")
    public ResponseEntity<Invoice> createInvoice(@Valid @RequestBody SaleCompletedRequest request) {

        Invoice invoice = invoiceService.createPendingInvoice(request);

        return ResponseEntity.ok(invoice);
    }
}
