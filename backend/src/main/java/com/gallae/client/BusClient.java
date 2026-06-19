package com.gallae.client;

import com.gallae.config.ExternalApiProperties;
import com.gallae.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusClient {

    private final WebClient.Builder webClientBuilder;
    private final ExternalApiProperties properties;

    public List<Map<String, Object>> fetchCityCodes() {
        checkServiceKey();
        log.debug("[BusClient] fetchCityCodes");
        throw new ExternalApiException("버스 API 키가 설정되지 않았습니다 (fallback 사용)");
    }

    public List<Map<String, Object>> fetchTerminals(String terminalName) {
        checkServiceKey();
        log.debug("[BusClient] fetchTerminals: {}", terminalName);
        throw new ExternalApiException("버스 API 키가 설정되지 않았습니다 (fallback 사용)");
    }

    public List<Map<String, Object>> fetchBusGrades() {
        checkServiceKey();
        log.debug("[BusClient] fetchBusGrades");
        throw new ExternalApiException("버스 API 키가 설정되지 않았습니다 (fallback 사용)");
    }

    public List<Map<String, Object>> fetchBusRoutes(String depCityCode, String arrCityCode, String date) {
        checkServiceKey();
        log.debug("[BusClient] fetchBusRoutes: {} -> {} on {}", depCityCode, arrCityCode, date);
        throw new ExternalApiException("버스 API 키가 설정되지 않았습니다 (fallback 사용)");
    }

    private void checkServiceKey() {
        String key = properties.getBus().getServiceKey();
        if (key == null || key.isBlank() || key.equals("PLACEHOLDER")) {
            throw new ExternalApiException("버스 API 키가 설정되지 않았습니다");
        }
    }
}
