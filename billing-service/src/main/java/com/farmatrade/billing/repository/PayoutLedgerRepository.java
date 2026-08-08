package com.farmatrade.billing.repository;

import com.farmatrade.billing.entity.PayoutLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayoutLedgerRepository extends JpaRepository<PayoutLedgerEntry, Long> {

    List<PayoutLedgerEntry> findByFarmerId(Long farmerId);
}