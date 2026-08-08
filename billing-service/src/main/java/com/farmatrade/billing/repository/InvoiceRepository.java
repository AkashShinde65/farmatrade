package com.farmatrade.billing.repository;

import com.farmatrade.billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByFarmerId(Long farmerId);

    List<Invoice> findByBuyerId(Long buyerId);

    Optional<Invoice> findByRazorpayOrderId(String razorpayOrderId);
}
