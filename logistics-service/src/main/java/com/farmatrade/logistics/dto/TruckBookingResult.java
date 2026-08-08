package com.farmatrade.logistics.dto;

import com.farmatrade.logistics.entity.TruckBookingStatus;

import java.time.LocalDateTime;

public record TruckBookingResult(
        Long truckId,
        String registrationNumber,
        double distanceKm,
        TruckBookingStatus status,
        LocalDateTime availableAgainAt
) {
}
