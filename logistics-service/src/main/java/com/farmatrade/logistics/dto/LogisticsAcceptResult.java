package com.farmatrade.logistics.dto;

import com.farmatrade.logistics.entity.LogisticsRequestStatus;

import java.util.List;

public record LogisticsAcceptResult(
        Long requestId,
        LogisticsRequestStatus status,
        List<ColdStorageMatch> coldStorageMatches,
        WeatherSnapshot weatherSnapshot,
        TruckBookingResult truckBooking
) {
}
