package com.gallae.service;

import com.gallae.client.KakaoNaviClient;
import com.gallae.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarService {

    private final KakaoNaviClient kakaoNaviClient;

    public Map<String, Object> getDirections(String origin, String destination) {
        try {
            return kakaoNaviClient.fetchDirections(origin, destination);
        } catch (ExternalApiException e) {
            log.warn("[CarService] 카카오 길찾기 실패: {}", e.getMessage());
            return getFallbackDirections(origin, destination);
        }
    }

    private Map<String, Object> getFallbackDirections(String origin, String destination) {
        Map<String, Object> result = new HashMap<>();
        result.put("origin", origin);
        result.put("destination", destination);
        result.put("durationMinutes", 280);
        result.put("distanceKm", 325.0);
        result.put("toll", 28100);
        result.put("fuelCost", 30000);
        result.put("note", "카카오 API 키 미설정 — 예상 데이터입니다");
        return result;
    }
}
