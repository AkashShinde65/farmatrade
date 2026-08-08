package com.farmatrade.billing.service;

import com.farmatrade.billing.entity.Invoice;
import com.farmatrade.billing.entity.PayoutLedgerEntry;
import com.farmatrade.billing.repository.PayoutLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayoutLedgerService {

    private final PayoutLedgerRepository payoutLedgerRepository;

    public void createLedgerEntry(Invoice invoice) {

        PayoutLedgerEntry entry = PayoutLedgerEntry.builder()
                .farmerId(invoice.getFarmerId())
                .invoiceId(invoice.getInvoiceId())
                .amount(invoice.getAmount())
                .status("PENDING")
                .build();

        payoutLedgerRepository.save(entry);
    }
}
