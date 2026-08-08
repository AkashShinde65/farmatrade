package com.farmatrade.logistics.service;

import com.farmatrade.logistics.dto.GeoPoint;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class GeocodingClient {

    private final RestClient nominatimRestClient;

    public GeocodingClient(RestClient nominatimRestClient) {
        this.nominatimRestClient = nominatimRestClient;
    }

    public GeoPoint geocode(String address) {
        List<Map<String, Object>> results = nominatimRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", address)
                        .queryParam("format", "json")
                        .queryParam("limit", 1)
                        .build())
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {
                });

        if (results == null || results.isEmpty()) {
            throw new NoSuchElementException("Could not geocode address: " + address);
        }

        Map<String, Object> first = results.get(0);
        double lat = Double.parseDouble(first.get("lat").toString());
        double lon = Double.parseDouble(first.get("lon").toString());
        return new GeoPoint(lat, lon);
    }
}
