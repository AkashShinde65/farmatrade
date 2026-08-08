package com.farmatrade.bidding.repository;

import com.farmatrade.bidding.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByLotId(Long lotId);

    List<Sale> findByStatus(Sale.SaleStatus status);

    List<Sale> findByBuyerId(Long buyerId);
}
