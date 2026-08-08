package com.farmatrade.logistics.service;

import com.farmatrade.logistics.dto.ColdStorageMatch;
import com.farmatrade.logistics.dto.GeoPoint;
import com.farmatrade.logistics.entity.ColdStorageFacility;
import com.farmatrade.logistics.exception.NoNearbyFacilityException;
import com.farmatrade.logistics.repository.ColdStorageFacilityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ColdStorageMatchingService {

    private final ColdStorageFacilityRepository facilityRepository;
    private final HaversineCalculator haversineCalculator;
    private final double maxDistanceKm;
    private final int maxResults;

    public ColdStorageMatchingService(
            ColdStorageFacilityRepository facilityRepository,
            HaversineCalculator haversineCalculator,
            @Value("${logistics.matching.max-distance-km}") double maxDistanceKm,
            @Value("${logistics.matching.max-results}") int maxResults) {
        this.facilityRepository = facilityRepository;
        this.haversineCalculator = haversineCalculator;
        this.maxDistanceKm = maxDistanceKm;
        this.maxResults = maxResults;
    }

    public List<ColdStorageMatch> findNearby(GeoPoint origin) {
        List<ColdStorageMatch> withinRange = facilityRepository.findAll().stream()
                .map(facility -> toMatch(facility, origin))
                .filter(match -> match.distanceKm() <= maxDistanceKm)
                .sorted(Comparator.comparingDouble(ColdStorageMatch::distanceKm))
                .limit(maxResults)
                .toList();

        if (withinRange.isEmpty()) {
            throw new NoNearbyFacilityException(
                    "No cold storage facility found within " + (int) maxDistanceKm + "km of the provided location.");
        }
        return withinRange;
    }

    private ColdStorageMatch toMatch(ColdStorageFacility facility, GeoPoint origin) {
        double distance = haversineCalculator.distanceKm(
                origin.latitude(), origin.longitude(), facility.getLatitude(), facility.getLongitude());
        return new ColdStorageMatch(
                facility.getId(), facility.getName(), facility.getState(), facility.getDistrict(),
                Math.round(distance * 100.0) / 100.0, facility.getCapacityTons());
    }
}
