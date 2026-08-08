package com.farmatrade.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleCompletedRequest {

    @NotNull(message = "Sale ID is required")
    @Positive(message = "Sale ID must be greater than 0")
    private Long saleId;

    @NotNull(message = "Buyer ID is required")
    @Positive(message = "Buyer ID must be greater than 0")
    private Long buyerId;

    @NotNull(message = "Farmer ID is required")
    @Positive(message = "Farmer ID must be greater than 0")
    private Long farmerId;

    @NotNull(message = "Lot ID is required")
    @Positive(message = "Lot ID must be greater than 0")
    private Long lotId;

    @NotBlank(message = "Crop name is required")
    private String cropName;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    private Double quantity;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private Double amount;

    @NotNull(message = "GST is required")
    @PositiveOrZero(message = "GST must be greater than or equal to 0")
    private Double gst;

    @NotNull(message = "Platform fee is required")
    @PositiveOrZero(message = "Platform fee must be greater than or equal to 0")
    private Double platformFee;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be greater than 0")
    private Double totalAmount;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotNull(message = "Logistics status is required")
    private Boolean logisticsAccepted;
}
