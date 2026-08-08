package com.farmatrade.logistics.controller;

import com.farmatrade.logistics.dto.ColdStorageMatch;
import com.farmatrade.logistics.dto.LogisticsAcceptResult;
import com.farmatrade.logistics.dto.LogisticsChoiceResponse;
import com.farmatrade.logistics.dto.OptInRequest;
import com.farmatrade.logistics.dto.TruckBookingResult;
import com.farmatrade.logistics.dto.WeatherRiskLevel;
import com.farmatrade.logistics.dto.WeatherSnapshot;
import com.farmatrade.logistics.entity.LogisticsRequest;
import com.farmatrade.logistics.entity.LogisticsRequestStatus;
import com.farmatrade.logistics.entity.TruckBookingStatus;
import com.farmatrade.logistics.service.LogisticsRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LogisticsRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class LogisticsRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LogisticsRequestService logisticsRequestService;

    @Test
    void createFromLotWin_returns201WithAWeatherSnapshot() throws Exception {
        OptInRequest requestBody = new OptInRequest(
                501L, 12L, 34L, "Village Road, Nashik, Maharashtra",
                9001L, BigDecimal.valueOf(5000), "Onion", 200.0,
                BigDecimal.valueOf(250), BigDecimal.valueOf(100), BigDecimal.valueOf(5350), "UPI");
        WeatherSnapshot snapshot = new WeatherSnapshot("Nashik, Maharashtra", 28.5, 62.0, WeatherRiskLevel.LOW, List.of());
        given(logisticsRequestService.createFromLotWin(requestBody)).willReturn(
                new LogisticsChoiceResponse(101L, 501L, LogisticsRequestStatus.PENDING_CHOICE, snapshot));

        mockMvc.perform(post("/api/logistics/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value(101))
                .andExpect(jsonPath("$.status").value("PENDING_CHOICE"))
                .andExpect(jsonPath("$.weatherSnapshot.riskLevel").value("LOW"));
    }

    @Test
    void createFromLotWin_returns400WhenFarmerAddressIsBlank() throws Exception {
        String invalidBody = """
                {"lotId": 501, "farmerId": 12, "buyerId": 34, "farmerAddress": ""}
                """;

        mockMvc.perform(post("/api/logistics/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_returnsTheRequest() throws Exception {
        LogisticsRequest request = new LogisticsRequest();
        request.setId(101L);
        request.setLotId(501L);
        request.setFarmerId(12L);
        request.setBuyerId(34L);
        request.setFarmerAddress("Village Road, Nashik, Maharashtra");
        request.setStatus(LogisticsRequestStatus.PENDING_CHOICE);
        given(logisticsRequestService.getById(101L)).willReturn(request);

        mockMvc.perform(get("/api/logistics/requests/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.farmerAddress").value("Village Road, Nashik, Maharashtra"));
    }

    @Test
    void getById_returns404WhenMissing() throws Exception {
        given(logisticsRequestService.getById(999L))
                .willThrow(new NoSuchElementException("No logistics request found with id 999"));

        mockMvc.perform(get("/api/logistics/requests/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void findByBuyer_returnsAllRequestsForThatBuyer() throws Exception {
        LogisticsRequest request = new LogisticsRequest();
        request.setId(101L);
        request.setLotId(501L);
        request.setFarmerId(12L);
        request.setBuyerId(34L);
        request.setFarmerAddress("Village Road, Nashik, Maharashtra");
        request.setStatus(LogisticsRequestStatus.PENDING_CHOICE);
        given(logisticsRequestService.findByBuyer(34L)).willReturn(List.of(request));

        mockMvc.perform(get("/api/logistics/requests").with(authentication(buyerAuth(34L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].buyerId").value(34));
    }

    @Test
    void accept_returnsMatchesWeatherAndTruckBooking() throws Exception {
        LocalDateTime eta = LocalDateTime.of(2026, 7, 27, 15, 30);
        LogisticsAcceptResult result = new LogisticsAcceptResult(
                101L,
                LogisticsRequestStatus.REQUESTED,
                List.of(new ColdStorageMatch(1L, "Nashik Cold Chain Hub", "Maharashtra", "Nashik", 4.32, 5000)),
                new WeatherSnapshot("Nashik, Maharashtra", 28.5, 62.0, WeatherRiskLevel.LOW, List.of()),
                new TruckBookingResult(7L, "MH-15-AB-1234", 4.32, TruckBookingStatus.BOOKED, eta));
        given(logisticsRequestService.accept(101L, 34L)).willReturn(result);

        mockMvc.perform(post("/api/logistics/requests/101/accept").with(authentication(buyerAuth(34L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.truckBooking.registrationNumber").value("MH-15-AB-1234"))
                .andExpect(jsonPath("$.coldStorageMatches", hasSize(1)));
    }

    @Test
    void accept_returns409WhenRequestIsNotPendingChoice() throws Exception {
        given(logisticsRequestService.accept(101L, 34L)).willThrow(
                new IllegalStateException("Logistics request 101 is REQUESTED, expected PENDING_CHOICE."));

        mockMvc.perform(post("/api/logistics/requests/101/accept").with(authentication(buyerAuth(34L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void decline_marksTheRequestDeclined() throws Exception {
        LogisticsRequest declined = new LogisticsRequest();
        declined.setId(101L);
        declined.setLotId(501L);
        declined.setFarmerId(12L);
        declined.setBuyerId(34L);
        declined.setFarmerAddress("Village Road, Nashik, Maharashtra");
        declined.setStatus(LogisticsRequestStatus.DECLINED);
        given(logisticsRequestService.decline(101L, 34L)).willReturn(declined);

        mockMvc.perform(post("/api/logistics/requests/101/decline").with(authentication(buyerAuth(34L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
    }

    private JwtAuthenticationToken buyerAuth(Long userId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("sub", String.valueOf(userId))
                .claim("role", "BUYER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_BUYER")));
    }
}
