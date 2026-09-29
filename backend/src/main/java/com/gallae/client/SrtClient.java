package com.gallae.client;

import com.gallae.config.ExternalApiProperties;
import com.gallae.dto.FareDto;
import com.gallae.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SrtClient {

    private final WebClient.Builder webClientBuilder;
    private final ExternalApiProperties properties;

    /**
     * SRT 여객 운임 조회 (공공데이터포털 정적 운임표)
     * GET api.odcloud.kr/api/15040160/v1/uddi:...?serviceKey=...&page=1&perPage=1000&returnType=JSON
     *
     * 응답: { "data": [{"출발역":"수서","도착역":"부산","일반실":52600,...},...], "totalCount": N }
     */
    public List<FareDto> fetchSrtFares(String from, String to) {
        String serviceKey = properties.getSrt().getServiceKey();
        if (serviceKey == null || serviceKey.isBlank() || serviceKey.equals("PLACEHOLDER")) {
            throw new ExternalApiException("SRT API 키가 설정되지 않았습니다");
        }
        String baseUrl = properties.getSrt().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ExternalApiException("SRT API URL이 설정되지 않았습니다");
        }

        log.debug("[SrtClient] fetchSrtFares: {} -> {}", from, to);
        try {
            Map<String, Object> response = webClientBuilder
                    .baseUrl(baseUrl)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("page", "1")
                            .queryParam("perPage", "1000")
                            .queryParam("returnType", "JSON")
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            return extractFares(response, from, to);
        } catch (WebClientResponseException e) {
            log.error("[SrtClient] HTTP 오류: {} {}", e.getStatusCode(), e.getMessage());
            throw new ExternalApiException("SRT API 호출 실패: " + e.getStatusCode(), e);
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[SrtClient] API 호출 실패: {}", e.getMessage());
            throw new ExternalApiException("SRT API 호출 실패", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<FareDto> extractFares(Map<String, Object> response, String from, String to) {
        if (response == null) return Collections.emptyList();
        try {
            Object dataObj = response.get("data");
            if (!(dataObj instanceof List)) return Collections.emptyList();
            List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;

            List<FareDto> result = new ArrayList<>();
            for (Map<String, Object> item : data) {
                String dep = getString(item, "출발역");
                String arr = getString(item, "도착역");
                if (dep == null || arr == null) continue;
                if (from != null && !dep.contains(from) && !from.contains(dep)) continue;
                if (to != null && !arr.contains(to) && !to.contains(arr)) continue;

                long fare = parseLong(item, "일반실");
                result.add(FareDto.builder()
                        .transportType("SRT")
                        .departureName(dep)
                        .arrivalName(arr)
                        .seatClass("standard")
                        .fare(fare)
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.warn("[SrtClient] 응답 파싱 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString().trim() : null;
    }

    private long parseLong(Map<String, Object> map, String key) {
        try {
            Object val = map.get(key);
            if (val == null) return 0L;
            return Long.parseLong(val.toString().replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0L;
        }
    }
}
