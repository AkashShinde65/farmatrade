package com.farmatrade.logistics.dto;

import java.util.List;

public record WeatherSnapshot(
        String locationName,
        double currentTempC,
        double currentHumidityPct,
        WeatherRiskLevel riskLevel,
        List<DailyForecast> forecast
) {
}
