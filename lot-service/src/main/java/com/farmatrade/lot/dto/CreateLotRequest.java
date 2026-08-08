package com.farmatrade.lot.dto;

import java.math.BigDecimal;

import com.farmatrade.lot.enums.SaleType;
import com.farmatrade.lot.enums.Unit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateLotRequest {
		@NotNull(message = "Farmer Id is required")
	    private Long farmerId;

		@NotBlank(message = "Crop Name is required")
		private String cropName;

		@NotBlank(message = "Grade is required")
		private String grade;

		@NotNull(message = "Quantity is required")
		@Positive(message = "Quantity must be greater than zero")
		private Double quantity;

	    @NotNull(message = "Unit is required")
	    private Unit unit;

	    @NotNull(message = "Sale Type is required")
	    private SaleType saleType;

	    private BigDecimal basePrice;

	    private BigDecimal fixedPrice;

	    private Integer auctionDurationMinutes;
	    
	    @NotNull(message = "Latitude is required")
	    private Double latitude;

	    @NotNull(message = "Longitude is required")
	    private Double longitude;

	    @NotBlank(message = "Location name is required")
	    private String locationName;
}
