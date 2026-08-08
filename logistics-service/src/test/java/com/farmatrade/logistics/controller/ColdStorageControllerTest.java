package com.farmatrade.logistics.controller;

import com.farmatrade.logistics.dto.ColdStorageMatch;
import com.farmatrade.logistics.dto.ColdStorageStateStat;
import com.farmatrade.logistics.dto.GeoPoint;
import com.farmatrade.logistics.exception.NoNearbyFacilityException;
import com.farmatrade.logistics.service.ColdStorageMatchingService;
import com.farmatrade.logistics.service.ColdStorageStateStatsService;
import com.farmatrade.logistics.service.GeocodingClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ColdStorageController.class)
@AutoConfigureMockMvc(addFilters = false)
class ColdStorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ColdStorageMatchingService coldStorageMatchingService;

    @MockBean
    private GeocodingClient geocodingClient;

    @MockBean
    private ColdStorageStateStatsService stateStatsService;

    @Test
    void nearby_returnsMatchesForAGeocodedAddress() throws Exception {
        given(geocodingClient.geocode("Nashik, Maharashtra")).willReturn(new GeoPoint(19.9975, 73.7898));
        given(coldStorageMatchingService.findNearby(new GeoPoint(19.9975, 73.7898))).willReturn(List.of(
                new ColdStorageMatch(1L, "Nashik Cold Chain Hub", "Maharashtra", "Nashik", 4.32, 5000),
                new ColdStorageMatch(2L, "Ozar Agro Storage", "Maharashtra", "Nashik", 11.05, 2200)
        ));

        mockMvc.perform(get("/api/logistics/cold-storage/nearby").param("address", "Nashik, Maharashtra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].facilityId").value(1))
                .andExpect(jsonPath("$[0].name").value("Nashik Cold Chain Hub"))
                .andExpect(jsonPath("$[0].distanceKm").value(4.32));
    }

    @Test
    void nearby_returns404WhenNoFacilityIsWithinRange() throws Exception {
        given(geocodingClient.geocode("Remote Village")).willReturn(new GeoPoint(10.0, 10.0));
        given(coldStorageMatchingService.findNearby(any())).willThrow(
                new NoNearbyFacilityException("No cold storage facility found within 100km of the provided location."));

        mockMvc.perform(get("/api/logistics/cold-storage/nearby").param("address", "Remote Village"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NO_NEARBY_FACILITY"));
    }

    @Test
    void statsForState_returnsTheStateStat() throws Exception {
        given(stateStatsService.getForState("Maharashtra"))
                .willReturn(new ColdStorageStateStat("Maharashtra", 720, 2_980_000));

        mockMvc.perform(get("/api/logistics/cold-storage/stats/Maharashtra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("Maharashtra"))
                .andExpect(jsonPath("$.registeredFacilityCount").value(720))
                .andExpect(jsonPath("$.totalCapacityTons").value(2_980_000));
    }

    @Test
    void statsForState_returns404ForAnUnknownState() throws Exception {
        given(stateStatsService.getForState("Atlantis"))
                .willThrow(new NoSuchElementException("No cold storage reference stat available for state: Atlantis"));

        mockMvc.perform(get("/api/logistics/cold-storage/stats/Atlantis"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void allStats_returnsTheFullList() throws Exception {
        given(stateStatsService.getAll()).willReturn(List.of(
                new ColdStorageStateStat("Gujarat", 890, 4_200_000),
                new ColdStorageStateStat("Maharashtra", 720, 2_980_000)
        ));

        mockMvc.perform(get("/api/logistics/cold-storage/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].state").value("Gujarat"));
    }
}
