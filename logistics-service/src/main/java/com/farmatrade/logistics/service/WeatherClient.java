package com.farmatrade.logistics.service;

import com.farmatrade.logistics.dto.DailyForecast;
import com.farmatrade.logistics.dto.GeoPoint;
import com.farmatrade.logistics.dto.WeatherSnapshot;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class WeatherClient {

    private final RestClient openMeteoRestClient;
    private final WeatherRiskCalculator weatherRiskCalculator;

    public WeatherClient(RestClient openMeteoRestClient, WeatherRiskCalculator weatherRiskCalculator) {
        this.openMeteoRestClient = openMeteoRestClient;
        this.weatherRiskCalculator = weatherRiskCalculator;
    }

    @SuppressWarnings("unchecked")
    public WeatherSnapshot getSnapshot(GeoPoint location, String locationName) {
        Map<String, Object> response = openMeteoRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/forecast")
                        .queryParam("latitude", location.latitude())
                        .queryParam("longitude", location.longitude())
                        .queryParam("current", "temperature_2m,relative_humidity_2m")
                        .queryParam("daily", "temperature_2m_min,temperature_2m_max,relative_humidity_2m_mean,precipitation_sum")
                        .queryParam("forecast_days", 3)
                        .queryParam("timezone", "auto")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });

        Map<String, Object> current = (Map<String, Object>) response.get("current");
        Map<String, Object> daily = (Map<String, Object>) response.get("daily");

        double currentTempC = toDouble(current.get("temperature_2m"));
        double currentHumidityPct = toDouble(current.get("relative_humidity_2m"));

        return new WeatherSnapshot(
                locationName,
                currentTempC,
                currentHumidityPct,
                weatherRiskCalculator.classify(currentTempC, currentHumidityPct),
                buildForecast(daily)
        );
    }

    @SuppressWarnings("unchecked")
    private List<DailyForecast> buildForecast(Map<String, Object> daily) {
        List<String> dates = (List<String>) daily.get("time");
        List<?> minTemps = (List<?>) daily.get("temperature_2m_min");
        List<?> maxTemps = (List<?>) daily.get("temperature_2m_max");
        List<?> avgHumidity = (List<?>) daily.get("relative_humidity_2m_mean");
        List<?> precipitation = (List<?>) daily.get("precipitation_sum");

        List<DailyForecast> forecast = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            forecast.add(new DailyForecast(
                    LocalDate.parse(dates.get(i)),
                    toDouble(minTemps.get(i)),
                    toDouble(maxTemps.get(i)),
                    toDouble(avgHumidity.get(i)),
                    toDouble(precipitation.get(i))
            ));
        }
        return forecast;
    }

    private double toDouble(Object value) {
        return value == null ? 0.0 : Double.parseDouble(value.toString());
    }
}
