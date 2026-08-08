package com.farmatrade.logistics.controller;

import com.farmatrade.logistics.dto.ColdStorageMatch;
import com.farmatrade.logistics.dto.ColdStorageStateStat;
import com.farmatrade.logistics.dto.GeoPoint;
import com.farmatrade.logistics.service.ColdStorageMatchingService;
import com.farmatrade.logistics.service.ColdStorageStateStatsService;
import com.farmatrade.logistics.service.GeocodingClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ColdStorageController {

    private final ColdStorageMatchingService coldStorageMatchingService;
    private final GeocodingClient geocodingClient;
    private final ColdStorageStateStatsService stateStatsService;

    public ColdStorageController(
            ColdStorageMatchingService coldStorageMatchingService,
            GeocodingClient geocodingClient,
            ColdStorageStateStatsService stateStatsService) {
        this.coldStorageMatchingService = coldStorageMatchingService;
        this.geocodingClient = geocodingClient;
        this.stateStatsService = stateStatsService;
    }

    @GetMapping("/api/logistics/cold-storage/nearby")
    public List<ColdStorageMatch> nearby(@RequestParam String address) {
        GeoPoint origin = geocodingClient.geocode(address);
        return coldStorageMatchingService.findNearby(origin);
    }

    @GetMapping("/api/logistics/cold-storage/stats/{state}")
    public ColdStorageStateStat statsForState(@PathVariable String state) {
        return stateStatsService.getForState(state);
    }

    @GetMapping("/api/logistics/cold-storage/stats")
    public List<ColdStorageStateStat> allStats() {
        return stateStatsService.getAll();
    }
}
