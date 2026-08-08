package com.farmatrade.lot.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.farmatrade.lot.enums.LotStatus;
import com.farmatrade.lot.enums.SaleType;
import com.farmatrade.lot.enums.Unit;

import lombok.Data;

@Data
public class LotResponse {
	 
	private Long id;

	private Long farmerId;

	private String cropName;

	private String grade;

	private Double quantity;

	private Unit unit;

	private SaleType saleType;

	private LotStatus status;
	
	private BigDecimal basePrice;

	private BigDecimal fixedPrice;

	private BigDecimal currentHighestBid;
	
	private Long currentHighestBidderId;
	
	private BigDecimal mandiReferencePrice;
	
	private WeatherSnapshot weather;

	private LocalDateTime auctionEndsAt;
	
	private Long winningBuyerId;

	private Double latitude;

	private Double longitude;

	private String locationName;
	
	private String imageUrl;

	
	public WeatherSnapshot getWeather() {
	    return weather;
	}
	
	public void setWeather(WeatherSnapshot weather) {
	    this.weather = weather;
	}
	
}
