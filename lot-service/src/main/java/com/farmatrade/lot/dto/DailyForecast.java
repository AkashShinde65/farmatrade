package com.farmatrade.lot.dto;

import java.time.LocalDate;

public class DailyForecast {

    private LocalDate date;

    private Double minTempC;

    private Double maxTempC;

    private Double avgHumidityPct;

    private Double precipitationMm;

    public DailyForecast() {
    }

    public DailyForecast(
            LocalDate date,
            Double minTempC,
            Double maxTempC,
            Double avgHumidityPct,
            Double precipitationMm) {

        this.date = date;
        this.minTempC = minTempC;
        this.maxTempC = maxTempC;
        this.avgHumidityPct = avgHumidityPct;
        this.precipitationMm = precipitationMm;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getMinTempC() {
        return minTempC;
    }

    public void setMinTempC(Double minTempC) {
        this.minTempC = minTempC;
    }

    public Double getMaxTempC() {
        return maxTempC;
    }

    public void setMaxTempC(Double maxTempC) {
        this.maxTempC = maxTempC;
    }

    public Double getAvgHumidityPct() {
        return avgHumidityPct;
    }

    public void setAvgHumidityPct(Double avgHumidityPct) {
        this.avgHumidityPct = avgHumidityPct;
    }

    public Double getPrecipitationMm() {
        return precipitationMm;
    }

    public void setPrecipitationMm(Double precipitationMm) {
        this.precipitationMm = precipitationMm;
    }
}