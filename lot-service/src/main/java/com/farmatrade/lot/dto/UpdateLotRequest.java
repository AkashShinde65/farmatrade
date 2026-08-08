package com.farmatrade.lot.dto;

import java.math.BigDecimal;

import com.farmatrade.lot.enums.SaleType;
import com.farmatrade.lot.enums.Unit;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class UpdateLotRequest {

    @NotBlank(message = "Crop name is required")
    private String cropName;

    @NotBlank(message = "Grade is required")
    private String grade;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Double quantity;

    @NotNull(message = "Unit is required")
    private Unit unit;

    @NotNull(message = "Sale type is required")
    private SaleType saleType;

    @DecimalMin(value = "0.01", message = "Base price must be greater than zero")
    private BigDecimal basePrice;

    @DecimalMin(value = "0.01", message = "Fixed price must be greater than zero")
    private BigDecimal fixedPrice;

    private Integer auctionDurationMinutes;

    private Double latitude;

    private Double longitude;

    private String locationName;

    // Getters and Setters

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public SaleType getSaleType() {
        return saleType;
    }

    public void setSaleType(SaleType saleType) {
        this.saleType = saleType;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getFixedPrice() {
        return fixedPrice;
    }

    public void setFixedPrice(BigDecimal fixedPrice) {
        this.fixedPrice = fixedPrice;
    }

    public Integer getAuctionDurationMinutes() {
        return auctionDurationMinutes;
    }

    public void setAuctionDurationMinutes(Integer auctionDurationMinutes) {
        this.auctionDurationMinutes = auctionDurationMinutes;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
}