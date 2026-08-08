package com.farmatrade.logistics.service;

import com.farmatrade.logistics.dto.WeatherRiskLevel;
import org.springframework.stereotype.Component;

/**
 * Kept as a single, isolated method on purpose so lot-service's own weather feature can copy
 * this exact rule later and stay consistent with logistics-service, rather than each service
 * inventing its own thresholds independently.
 */
@Component
public class WeatherRiskCalculator {

    public WeatherRiskLevel classify(double currentTempC, double currentHumidityPct) {
        if (currentTempC > 35 || currentHumidityPct > 80) {
            return WeatherRiskLevel.HIGH;
        }
        if ((currentTempC >= 25 && currentTempC <= 35) || (currentHumidityPct >= 60 && currentHumidityPct <= 80)) {
            return WeatherRiskLevel.MEDIUM;
        }
        return WeatherRiskLevel.LOW;
    }
}
