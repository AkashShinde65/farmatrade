package com.farmatrade.logistics;

import com.farmatrade.logistics.dto.GeoPoint;
import com.farmatrade.logistics.dto.TruckBookingResult;
import com.farmatrade.logistics.entity.LogisticsRequest;
import com.farmatrade.logistics.entity.LogisticsRequestStatus;
import com.farmatrade.logistics.entity.Truck;
import com.farmatrade.logistics.entity.TruckStatus;
import com.farmatrade.logistics.repository.LogisticsRequestRepository;
import com.farmatrade.logistics.repository.TruckRepository;
import com.farmatrade.logistics.service.TruckMockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real booking code path (TruckMockService.bookNearestAvailable, the atomic
 * conditional UPDATE it relies on) against the actual configured database -- not a simulation.
 * Requires a reachable MySQL matching application.properties; there is no Testcontainers setup
 * yet, so this runs against whatever datasource is configured (see DB_URL).
 */
@SpringBootTest
class TruckBookingLifecycleIT {

    @Autowired
    private TruckMockService truckMockService;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private LogisticsRequestRepository logisticsRequestRepository;

    @Test
    void bookingReservesTheNearestTruckAndMakesItUnavailable() {
        List<Truck> availableBefore = truckRepository.findByStatus(TruckStatus.AVAILABLE);
        assertThat(availableBefore).isNotEmpty();
        Truck nearest = availableBefore.get(0);
        GeoPoint pickup = new GeoPoint(nearest.getLatitude(), nearest.getLongitude());

        LogisticsRequest request = new LogisticsRequest();
        request.setLotId(999_001L);
        request.setFarmerId(1L);
        request.setBuyerId(1L);
        request.setFarmerAddress("TruckBookingLifecycleIT test address");
        request.setLatitude(pickup.latitude());
        request.setLongitude(pickup.longitude());
        request.setStatus(LogisticsRequestStatus.PENDING_CHOICE);
        request = logisticsRequestRepository.save(request);

        TruckBookingResult result = truckMockService.bookNearestAvailable(pickup, request.getId());

        assertThat(result).isNotNull();
        assertThat(result.distanceKm()).isEqualTo(0.0);

        Truck booked = truckRepository.findById(result.truckId()).orElseThrow();
        assertThat(booked.getStatus()).isEqualTo(TruckStatus.BOOKED);
        assertThat(booked.getCurrentLogisticsRequestId()).isEqualTo(request.getId());
        assertThat(booked.getAvailableAgainAt()).isNotNull().isAfter(LocalDateTime.now());

        List<Truck> availableAfter = truckRepository.findByStatus(TruckStatus.AVAILABLE);
        assertThat(availableAfter).extracting(Truck::getId).doesNotContain(booked.getId());

        // Fast-forward the mock ETA into the past so TruckReleaseScheduler's next real tick
        // (it runs every 60s inside the live app) picks this truck up and releases it on its own.
        booked.setAvailableAgainAt(LocalDateTime.now().minusMinutes(1));
        truckRepository.save(booked);

        System.out.println("BOOKED_TRUCK_ID=" + booked.getId());
        System.out.println("BOOKED_REQUEST_ID=" + request.getId());
    }
}
