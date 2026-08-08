package com.farmatrade.lot.dto;

import java.util.List;

public class WeatherSnapshot {

    private String locationName;

    private Double currentTempC;

    private Double currentHumidityPct;

    private String riskLevel;

    private List<DailyForecast> forecast;

    public WeatherSnapshot() {
    }

    public WeatherSnapshot(
            String locationName,
            Double currentTempC,
            Double currentHumidityPct,
            String riskLevel,
            List<DailyForecast> forecast) {

        this.locationName = locationName;
        this.currentTempC = currentTempC;
        this.currentHumidityPct = currentHumidityPct;
        this.riskLevel = riskLevel;
        this.forecast = forecast;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Double getCurrentTempC() {
        return currentTempC;
    }

    public void setCurrentTempC(Double currentTempC) {
        this.currentTempC = currentTempC;
    }

    public Double getCurrentHumidityPct() {
        return currentHumidityPct;
    }

    public void setCurrentHumidityPct(Double currentHumidityPct) {
        this.currentHumidityPct = currentHumidityPct;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<DailyForecast> getForecast() {
        return forecast;
    }

    public void setForecast(List<DailyForecast> forecast) {
        this.forecast = forecast;
    }
}