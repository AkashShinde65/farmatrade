package com.farmatrade.logistics.service;

import com.farmatrade.logistics.dto.ColdStorageStateStat;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

/**
 * Static, state-level "X facilities registered in your state" display stat, modeled on the
 * structure of data.gov.in's cold storage capacity dataset (state, registered facility count,
 * total capacity in tons). This is illustrative reference data captured once, NOT fetched live
 * at runtime, and is intentionally independent of the {@code cold_storage_facility} table used
 * by ColdStorageMatchingService -- the two are unrelated data sources by design.
 */
@Service
public class ColdStorageStateStatsService {

    private final Map<String, ColdStorageStateStat> statsByState = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public ColdStorageStateStatsService() {
        register("Andhra Pradesh", 620, 2_150_000);
        register("Assam", 60, 210_000);
        register("Bihar", 250, 1_450_000);
        register("Chhattisgarh", 110, 380_000);
        register("Delhi", 90, 260_000);
        register("Gujarat", 890, 4_200_000);
        register("Haryana", 340, 1_950_000);
        register("Himachal Pradesh", 45, 95_000);
        register("Jammu & Kashmir", 55, 180_000);
        register("Jharkhand", 40, 130_000);
        register("Karnataka", 310, 1_320_000);
        register("Kerala", 180, 540_000);
        register("Madhya Pradesh", 280, 1_180_000);
        register("Maharashtra", 720, 2_980_000);
        register("Odisha", 70, 240_000);
        register("Punjab", 780, 3_600_000);
        register("Rajasthan", 230, 890_000);
        register("Tamil Nadu", 260, 1_050_000);
        register("Telangana", 150, 610_000);
        register("Uttar Pradesh", 2450, 15_800_000);
        register("Uttarakhand", 65, 220_000);
        register("West Bengal", 640, 3_450_000);
    }

    private void register(String state, int registeredFacilityCount, long totalCapacityTons) {
        statsByState.put(state, new ColdStorageStateStat(state, registeredFacilityCount, totalCapacityTons));
    }

    public ColdStorageStateStat getForState(String state) {
        ColdStorageStateStat stat = statsByState.get(state);
        if (stat == null) {
            throw new NoSuchElementException("No cold storage reference stat available for state: " + state);
        }
        return stat;
    }

    public List<ColdStorageStateStat> getAll() {
        return List.copyOf(statsByState.values());
    }
}
