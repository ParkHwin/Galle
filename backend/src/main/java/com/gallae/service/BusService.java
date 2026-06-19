package com.gallae.service;

import com.gallae.client.BusClient;
import com.gallae.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusService {

    private final BusClient busClient;

    public List<Map<String, Object>> getCities() {
        try {
            return busClient.fetchCityCodes();
        } catch (ExternalApiException e) {
            log.warn("[BusService] 도시코드 조회 실패: {}", e.getMessage());
            return getDefaultCities();
        }
    }

    public List<Map<String, Object>> getTerminals(String name) {
        try {
            return busClient.fetchTerminals(name);
        } catch (ExternalApiException e) {
            log.warn("[BusService] 터미널 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getGrades() {
        try {
            return busClient.fetchBusGrades();
        } catch (ExternalApiException e) {
            log.warn("[BusService] 등급 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getRoutes(String depCityCode, String arrCityCode, String date) {
        try {
            return busClient.fetchBusRoutes(depCityCode, arrCityCode, date);
        } catch (ExternalApiException e) {
            log.warn("[BusService] 버스노선 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> getDefaultCities() {
        List<Map<String, Object>> cities = new ArrayList<>();
        String[] cityNames = {"서울", "부산", "대전", "대구", "광주", "제주"};
        String[] cityCodes = {"1", "2", "3", "4", "5", "6"};
        for (int i = 0; i < cityNames.length; i++) {
            Map<String, Object> city = new HashMap<>();
            city.put("cityName", cityNames[i]);
            city.put("cityCode", cityCodes[i]);
            cities.add(city);
        }
        return cities;
    }
}
