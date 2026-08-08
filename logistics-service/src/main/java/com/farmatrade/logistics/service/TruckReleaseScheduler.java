package com.farmatrade.logistics.service;

import com.farmatrade.logistics.entity.LogisticsRequest;
import com.farmatrade.logistics.entity.Truck;
import com.farmatrade.logistics.entity.TruckBookingStatus;
import com.farmatrade.logistics.entity.TruckStatus;
import com.farmatrade.logistics.repository.LogisticsRequestRepository;
import com.farmatrade.logistics.repository.TruckRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Simulates trucks genuinely returning from delivery without anyone manually clicking
 * "advance". Runs every minute, finds any truck still BOOKED whose mock ETA has passed, frees
 * it, and marks its logistics request DELIVERED -- the same end state advanceStatus() reaches
 * manually, just triggered by time instead of an API call.
 */
@Component
public class TruckReleaseScheduler {

    private final TruckRepository truckRepository;
    private final LogisticsRequestRepository logisticsRequestRepository;

    public TruckReleaseScheduler(TruckRepository truckRepository, LogisticsRequestRepository logisticsRequestRepository) {
        this.truckRepository = truckRepository;
        this.logisticsRequestRepository = logisticsRequestRepository;
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void releaseOverdueTrucks() {
        List<Truck> overdue = truckRepository.findByStatusAndAvailableAgainAtBefore(
                TruckStatus.BOOKED, LocalDateTime.now());

        for (Truck truck : overdue) {
            Long requestId = truck.getCurrentLogisticsRequestId();

            truckRepository.release(truck.getId());

            if (requestId != null) {
                logisticsRequestRepository.findById(requestId).ifPresent(this::markDelivered);
            }
        }
    }

    private void markDelivered(LogisticsRequest request) {
        if (request.getTruckBookingStatus() != null && request.getTruckBookingStatus() != TruckBookingStatus.DELIVERED) {
            request.setTruckBookingStatus(TruckBookingStatus.DELIVERED);
            logisticsRequestRepository.save(request);
        }
    }
}
