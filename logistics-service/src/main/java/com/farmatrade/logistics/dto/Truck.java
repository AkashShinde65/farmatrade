package com.farmatrade.logistics.dto;

import com.farmatrade.logistics.entity.TruckStatus;

import java.time.LocalDateTime;

public record Truck(
        Long id,
        String registrationNumber,
        String state,
        String district,
        double latitude,
        double longitude,
        TruckStatus status,
        Long currentLogisticsRequestId,
        LocalDateTime availableAgainAt
) {
}
