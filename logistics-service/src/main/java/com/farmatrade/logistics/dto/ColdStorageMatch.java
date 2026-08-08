package com.farmatrade.logistics.dto;

public record ColdStorageMatch(
        Long facilityId,
        String name,
        String state,
        String district,
        double distanceKm,
        int capacityTons
) {
}
