package com.farmatrade.logistics.repository;

import com.farmatrade.logistics.entity.LogisticsRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogisticsRequestRepository extends JpaRepository<LogisticsRequest, Long> {

    List<LogisticsRequest> findByBuyerId(Long buyerId);

    List<LogisticsRequest> findByLotId(Long lotId);
}
