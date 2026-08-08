package com.farmatrade.logistics.controller;

import com.farmatrade.logistics.dto.Truck;
import com.farmatrade.logistics.entity.LogisticsRequest;
import com.farmatrade.logistics.service.TruckMockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TruckBookingController {

    private final TruckMockService truckMockService;

    public TruckBookingController(TruckMockService truckMockService) {
        this.truckMockService = truckMockService;
    }

    @GetMapping("/api/logistics/truck-bookings/fleet")
    public List<Truck> fleet() {
        return truckMockService.getFleet();
    }

    /**
     * Admin/mock-only endpoint: advances a logistics request's truck booking status
     * BOOKED -> PICKED_UP -> DELIVERED. {id} is the logistics request id (a booking is just a
     * status + truck reference on that request now, not a separate object). There is no real
     * carrier API behind this; it exists purely to simulate progress, alongside
     * TruckReleaseScheduler which does the same thing automatically once the mock ETA passes.
     */
    @PostMapping("/api/logistics/truck-bookings/{id}/advance")
    public LogisticsRequest advance(@PathVariable Long id) {
        return truckMockService.advanceStatus(id);
    }
}
