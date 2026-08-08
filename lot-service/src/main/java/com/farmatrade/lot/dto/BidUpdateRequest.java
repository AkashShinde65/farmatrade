package com.farmatrade.lot.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class BidUpdateRequest {
	 private Long lotId;

	 private BigDecimal highestBid;

	 private Long highestBidderId;
}
