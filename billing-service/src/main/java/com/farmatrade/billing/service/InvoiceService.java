package com.farmatrade.billing.service;

import com.farmatrade.billing.dto.SaleCompletedRequest;
import com.farmatrade.billing.entity.Invoice;
import com.farmatrade.billing.entity.InvoiceStatus;
import com.farmatrade.billing.exception.InvoiceNotFoundException;
import com.farmatrade.billing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public Invoice createPendingInvoice(SaleCompletedRequest request) {

        Invoice invoice = Invoice.builder()
                .saleId(request.getSaleId())
                .buyerId(request.getBuyerId())
                .farmerId(request.getFarmerId())
                .lotId(request.getLotId())
                .cropName(request.getCropName())
                .quantity(request.getQuantity())
                .amount(request.getAmount())
                .gst(request.getGst())
                .platformFee(request.getPlatformFee())
                .totalAmount(request.getTotalAmount())
                .paymentMethod(request.getPaymentMethod())
                .logisticsAccepted(request.getLogisticsAccepted())
                .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(InvoiceStatus.PENDING)
                .build();

        return invoiceRepository.save(invoice);
    }

    public Invoice getInvoice(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() ->
                        new InvoiceNotFoundException("Invoice not found with ID: " + invoiceId));
    }

    public List<Invoice> getBuyerInvoices(Long buyerId) {
        return invoiceRepository.findByBuyerId(buyerId);
    }

    public List<Invoice> getFarmerInvoices(Long farmerId) {
        return invoiceRepository.findByFarmerId(farmerId);
    }
}
