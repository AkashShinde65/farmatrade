package com.farmatrade.logistics.service;

import com.farmatrade.logistics.dto.GeoPoint;
import com.farmatrade.logistics.dto.Truck;
import com.farmatrade.logistics.dto.TruckBookingResult;
import com.farmatrade.logistics.entity.LogisticsRequest;
import com.farmatrade.logistics.entity.TruckBookingStatus;
import com.farmatrade.logistics.entity.TruckStatus;
import com.farmatrade.logistics.repository.LogisticsRequestRepository;
import com.farmatrade.logistics.repository.TruckRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Real, MySQL-persisted truck fleet and bookings -- no in-memory fake fleet. "Mock" now only
 * describes that there's still no real carrier/telematics API behind status advancement and ETA;
 * the fleet and booking state themselves are genuine, durable rows that survive a restart.
 */
@Service
public class TruckMockService {

    private final TruckRepository truckRepository;
    private final LogisticsRequestRepository logisticsRequestRepository;
    private final HaversineCalculator haversineCalculator;
    private final double averageSpeedKmh;
    private final long minEtaMinutes;

    public TruckMockService(
            TruckRepository truckRepository,
            LogisticsRequestRepository logisticsRequestRepository,
            HaversineCalculator haversineCalculator,
            @Value("${logistics.truck.average-speed-kmh}") double averageSpeedKmh,
            @Value("${logistics.truck.min-eta-minutes}") long minEtaMinutes) {
        this.truckRepository = truckRepository;
        this.logisticsRequestRepository = logisticsRequestRepository;
        this.haversineCalculator = haversineCalculator;
        this.averageSpeedKmh = averageSpeedKmh;
        this.minEtaMinutes = minEtaMinutes;
    }

    public List<Truck> getFleet() {
        return truckRepository.findAll().stream().map(this::toDto).toList();
    }

    /**
     * Finds the nearest AVAILABLE truck and attempts an atomic conditional booking. If another
     * request wins the race for the nearest truck first (0 rows affected), falls through to the
     * next-nearest AVAILABLE truck instead of failing outright.
     */
    @Transactional
    public TruckBookingResult bookNearestAvailable(GeoPoint pickup, Long logisticsRequestId) {
        List<CandidateTruck> candidates = truckRepository.findByStatus(TruckStatus.AVAILABLE).stream()
                .map(truck -> new CandidateTruck(truck, haversineCalculator.distanceKm(
                        pickup.latitude(), pickup.longitude(), truck.getLatitude(), truck.getLongitude())))
                .sorted(Comparator.comparingDouble(CandidateTruck::distanceKm))
                .toList();

        for (CandidateTruck candidate : candidates) {
            long etaMinutes = mockEtaMinutes(candidate.distanceKm());
            LocalDateTime availableAgainAt = LocalDateTime.now().plusMinutes(etaMinutes);

            int updated = truckRepository.tryBook(candidate.truck().getId(), logisticsRequestId, availableAgainAt);
            if (updated == 1) {
                return new TruckBookingResult(
                        candidate.truck().getId(),
                        candidate.truck().getRegistrationNumber(),
                        Math.round(candidate.distanceKm() * 100.0) / 100.0,
                        TruckBookingStatus.BOOKED,
                        availableAgainAt);
            }
            // 0 rows affected: another request booked this truck between our SELECT and UPDATE. Try the next-nearest.
        }

        throw new NoSuchElementException("No available truck found near the pickup location.");
    }

    /**
     * Advances the buyer-facing TruckBookingStatus on a logistics request: BOOKED -> PICKED_UP
     * -> DELIVERED. Releasing the truck back to AVAILABLE happens here when DELIVERED is reached
     * manually; TruckReleaseScheduler does the same thing automatically once the mock ETA passes.
     */
    @Transactional
    public LogisticsRequest advanceStatus(Long logisticsRequestId) {
        LogisticsRequest request = logisticsRequestRepository.findById(logisticsRequestId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No logistics request found with id " + logisticsRequestId));

        TruckBookingStatus current = request.getTruckBookingStatus();
        if (current == null) {
            throw new IllegalStateException(
                    "Logistics request " + logisticsRequestId + " has no truck booking to advance.");
        }

        TruckBookingStatus next = switch (current) {
            case BOOKED -> TruckBookingStatus.PICKED_UP;
            case PICKED_UP -> TruckBookingStatus.DELIVERED;
            case DELIVERED -> throw new IllegalStateException(
                    "Logistics request " + logisticsRequestId
                            + "'s truck booking is already DELIVERED and cannot advance further.");
        };

        request.setTruckBookingStatus(next);
        if (next == TruckBookingStatus.DELIVERED && request.getTruckId() != null) {
            truckRepository.release(request.getTruckId());
        }
        return logisticsRequestRepository.save(request);
    }

    private long mockEtaMinutes(double distanceKm) {
        long minutes = Math.round((distanceKm / averageSpeedKmh) * 60.0);
        return Math.max(minutes, minEtaMinutes);
    }

    private Truck toDto(com.farmatrade.logistics.entity.Truck truck) {
        return new Truck(truck.getId(), truck.getRegistrationNumber(), truck.getState(), truck.getDistrict(),
                truck.getLatitude(), truck.getLongitude(), truck.getStatus(),
                truck.getCurrentLogisticsRequestId(), truck.getAvailableAgainAt());
    }

    private record CandidateTruck(com.farmatrade.logistics.entity.Truck truck, double distanceKm) {
    }
}
