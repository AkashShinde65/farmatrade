package com.farmatrade.logistics.dto;

import java.time.LocalDate;

public record DailyForecast(
        LocalDate date,
        double minTempC,
        double maxTempC,
        double avgHumidityPct,
        double precipitationMm
) {
}
