package com.farmatrade.logistics.dto;

public record ColdStorageStateStat(
        String state,
        int registeredFacilityCount,
        long totalCapacityTons
) {
}
