package com.farmatrade.lot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.farmatrade.lot.entity.Lot;
import com.farmatrade.lot.enums.LotStatus;
import com.farmatrade.lot.enums.SaleType;

public interface LotRepository extends JpaRepository<Lot, Long>{
		
		List<Lot> findByFarmerId(Long farmerId);

	    List<Lot> findByStatus(LotStatus status);
	    
	    List<Lot> findBySaleType(SaleType saleType);
	    
	    List<Lot> findByWinningBuyerId(Long winningBuyerId);

	    List<Lot> findByCropNameContainingIgnoreCase(String cropName);
	    
}
