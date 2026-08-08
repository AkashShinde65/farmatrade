package com.farmatrade.logistics.controller;

import com.farmatrade.logistics.dto.Truck;
import com.farmatrade.logistics.entity.LogisticsRequest;
import com.farmatrade.logistics.entity.LogisticsRequestStatus;
import com.farmatrade.logistics.entity.TruckBookingStatus;
import com.farmatrade.logistics.entity.TruckStatus;
import com.farmatrade.logistics.service.TruckMockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TruckBookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class TruckBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TruckMockService truckMockService;

    @Test
    void fleet_returnsAllTrucks() throws Exception {
        given(truckMockService.getFleet()).willReturn(List.of(
                new Truck(7L, "MH-15-AB-1234", "Maharashtra", "Nashik", 19.99, 73.78,
                        TruckStatus.AVAILABLE, null, null)
        ));

        mockMvc.perform(get("/api/logistics/truck-bookings/fleet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].registrationNumber").value("MH-15-AB-1234"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void advance_movesBookedToPickedUp() throws Exception {
        LogisticsRequest request = new LogisticsRequest();
        request.setId(101L);
        request.setLotId(501L);
        request.setFarmerId(12L);
        request.setBuyerId(34L);
        request.setFarmerAddress("Village Road, Nashik, Maharashtra");
        request.setStatus(LogisticsRequestStatus.REQUESTED);
        request.setTruckId(7L);
        request.setTruckBookingStatus(TruckBookingStatus.PICKED_UP);
        given(truckMockService.advanceStatus(101L)).willReturn(request);

        mockMvc.perform(post("/api/logistics/truck-bookings/101/advance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.truckBookingStatus").value("PICKED_UP"));
    }

    @Test
    void advance_returns409WhenAlreadyDelivered() throws Exception {
        given(truckMockService.advanceStatus(101L)).willThrow(new IllegalStateException(
                "Logistics request 101's truck booking is already DELIVERED and cannot advance further."));

        mockMvc.perform(post("/api/logistics/truck-bookings/101/advance"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void advance_returns404WhenRequestMissing() throws Exception {
        given(truckMockService.advanceStatus(999L)).willThrow(
                new NoSuchElementException("No logistics request found with id 999"));

        mockMvc.perform(post("/api/logistics/truck-bookings/999/advance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
