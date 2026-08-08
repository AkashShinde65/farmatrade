package com.farmatrade.logistics.dto;

import com.farmatrade.logistics.entity.LogisticsRequestStatus;

/**
 * What the buyer sees right after a lot is won, while deciding whether to accept or decline
 * logistics help -- includes a weather snapshot for the pickup location so that decision is
 * informed, before any cold storage matching or truck booking happens.
 */
public record LogisticsChoiceResponse(
        Long requestId,
        Long lotId,
        LogisticsRequestStatus status,
        WeatherSnapshot weatherSnapshot
) {
}
