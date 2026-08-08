package com.farmatrade.lot.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.farmatrade.lot.dto.DailyForecast;
import com.farmatrade.lot.dto.WeatherSnapshot;

@Service
public class WeatherClient {

    private static final Logger log = LoggerFactory.getLogger(WeatherClient.class);

    private final RestClient restClient;

    public WeatherClient(RestClient.Builder builder) {

        this.restClient = builder
                .baseUrl("https://api.open-meteo.com")
                .build();
    }

    /**
     * CORRECTED 2026-08-01 -- previously had no error handling at all, unlike MandiPriceClient's
     * equivalent method: a lot created with missing latitude/longitude (both optional on
     * CreateLotRequest) crashed the entire lot-creation request with a NullPointerException
     * instead of just omitting weather, confirmed live during integration testing. Now mirrors
     * MandiPriceClient's defensive pattern -- best-effort, never blocks lot creation.
     */
    @Cacheable(
            value = "weather",
            key = "#latitude + ':' + #longitude"
    )
    public WeatherSnapshot getWeather(
            Double latitude,
            Double longitude,
            String locationName) {

        if (latitude == null || longitude == null) {
            log.warn("Skipping weather lookup for '{}' -- latitude/longitude not provided", locationName);
            return null;
        }

        try {

            Map<String, Object> response =
                    restClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/v1/forecast")
                                    .queryParam("latitude", latitude)
                                    .queryParam("longitude", longitude)
                                    .queryParam(
                                            "current",
                                            "temperature_2m,relative_humidity_2m"
                                    )
                                    .queryParam(
                                            "daily",
                                            "temperature_2m_min,temperature_2m_max,relative_humidity_2m_mean,precipitation_sum"
                                    )
                                    .queryParam("forecast_days", 3)
                                    .queryParam("timezone", "auto")
                                    .build())
                            .retrieve()
                            .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            return buildWeatherSnapshot(
                    response,
                    locationName
            );

        } catch (Exception e) {

            log.error("Weather API error for location '{}'", locationName, e);

            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private WeatherSnapshot buildWeatherSnapshot(
            Map<String, Object> response,
            String locationName) {

        Map<String, Object> current =
                (Map<String, Object>) response.get("current");

        Double currentTemp =
                ((Number) current.get("temperature_2m"))
                        .doubleValue();

        Double currentHumidity =
                ((Number) current.get("relative_humidity_2m"))
                        .doubleValue();

        String riskLevel =
                calculateRisk(
                        currentTemp,
                        currentHumidity
                );

        Map<String, Object> daily =
                (Map<String, Object>) response.get("daily");

        List<String> dates =
                (List<String>) daily.get("time");

        List<Double> minTemps =
                toDoubleList(
                        (List<?>) daily.get("temperature_2m_min")
                );

        List<Double> maxTemps =
                toDoubleList(
                        (List<?>) daily.get("temperature_2m_max")
                );

        List<Double> humidities =
                toDoubleList(
                        (List<?>) daily.get("relative_humidity_2m_mean")
                );

        List<Double> rainfall =
                toDoubleList(
                        (List<?>) daily.get("precipitation_sum")
                );

        List<DailyForecast> forecasts =
                new ArrayList<>();

        for (int i = 0; i < dates.size(); i++) {

            forecasts.add(
                    new DailyForecast(
                            LocalDate.parse(dates.get(i)),
                            minTemps.get(i),
                            maxTemps.get(i),
                            humidities.get(i),
                            rainfall.get(i)
                    )
            );
        }

        return new WeatherSnapshot(
                locationName,
                currentTemp,
                currentHumidity,
                riskLevel,
                forecasts
        );
    }

    private List<Double> toDoubleList(List<?> values) {

        List<Double> result = new ArrayList<>();

        for (Object value : values) {

            result.add(
                    value == null
                            ? 0.0
                            : ((Number) value).doubleValue()
            );
        }

        return result;
    }

    private String calculateRisk(
            Double temperature,
            Double humidity) {

        if (temperature > 35
                || humidity > 80) {

            return "HIGH";
        }

        if ((temperature >= 25 && temperature <= 35)
                || (humidity >= 60 && humidity <= 80)) {

            return "MEDIUM";
        }

        return "LOW";
    }
}