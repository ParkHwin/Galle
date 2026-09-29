package com.gallae.client;

import com.gallae.config.ExternalApiProperties;
import com.gallae.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoNaviClient {

    private final WebClient.Builder webClientBuilder;
    private final ExternalApiProperties properties;

    /**
     * 카카오 모빌리티 길찾기 API
     * GET {naviBaseUrl}/v1/directions?origin={lon,lat}&destination={lon,lat}
     *
     * ※ origin/destination은 "경도,위도" 형식이어야 합니다.
     *    예: "127.1054221,37.5140475" (서울역)
     *    도시명이 넘어오면 geocoding 전처리가 필요합니다.
     */
    public Map<String, Object> fetchDirections(String origin, String destination) {
        String apiKey = properties.getKakao().getApiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("PLACEHOLDER")) {
            throw new ExternalApiException("카카오 API 키가 설정되지 않았습니다 (fallback 사용)");
        }

        // origin/destination이 좌표 형식("lon,lat")이 아니면 fallback으로 넘어감
        if (!isCoordinate(origin) || !isCoordinate(destination)) {
            throw new ExternalApiException(
                "카카오 Navi API는 좌표(경도,위도) 형식이 필요합니다. 입력값: origin=" + origin + ", destination=" + destination
            );
        }

        log.debug("[KakaoNaviClient] fetchDirections: {} -> {}", origin, destination);
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("apis-navi.kakaomobility.com")
                            .path("/v1/directions")
                            .queryParam("origin", origin)
                            .queryParam("destination", destination)
                            .queryParam("priority", "RECOMMEND")
                            .queryParam("car_type", "1")
                            .build())
                    .header("Authorization", "KakaoAK " + apiKey)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("[KakaoNaviClient] HTTP 오류: {} {}", e.getStatusCode(), e.getMessage());
            throw new ExternalApiException("카카오 API 호출 실패: " + e.getStatusCode(), e);
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[KakaoNaviClient] 호출 실패: {}", e.getMessage());
            throw new ExternalApiException("카카오 API 호출 실패", e);
        }
    }

    /** "경도,위도" 형식인지 간단 체크 */
    private boolean isCoordinate(String value) {
        if (value == null) return false;
        String[] parts = value.split(",");
        if (parts.length != 2) return false;
        try {
            Double.parseDouble(parts[0].trim());
            Double.parseDouble(parts[1].trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
