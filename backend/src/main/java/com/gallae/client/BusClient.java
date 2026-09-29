package com.gallae.client;

import com.gallae.config.ExternalApiProperties;
import com.gallae.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusClient {

    private final WebClient.Builder webClientBuilder;
    private final ExternalApiProperties properties;

    /**
     * 고속버스 도시 코드 목록 조회
     * GET {baseUrl}/getExpBusCityList2?serviceKey=...&_type=json
     */
    public List<Map<String, Object>> fetchCityCodes() {
        String key = getValidServiceKey();
        log.debug("[BusClient] fetchCityCodes");
        try {
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("apis.data.go.kr")
                            .path("/1613000/ExpBusInfoService2/getExpBusCityList2")
                            .queryParam("serviceKey", key)
                            .queryParam("_type", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            return extractItems(response);
        } catch (WebClientResponseException e) {
            log.error("[BusClient] fetchCityCodes HTTP 오류: {} {}", e.getStatusCode(), e.getMessage());
            throw new ExternalApiException("버스 도시 코드 조회 실패: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("[BusClient] fetchCityCodes 실패: {}", e.getMessage());
            throw new ExternalApiException("버스 도시 코드 조회 실패", e);
        }
    }

    /**
     * 터미널 목록 조회
     * GET {baseUrl}/getExpBusTerminalList2?serviceKey=...&cityCode=...&_type=json
     */
    public List<Map<String, Object>> fetchTerminals(String cityCode) {
        String key = getValidServiceKey();
        log.debug("[BusClient] fetchTerminals: cityCode={}", cityCode);
        try {
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("apis.data.go.kr")
                            .path("/1613000/ExpBusInfoService2/getExpBusTerminalList2")
                            .queryParam("serviceKey", key)
                            .queryParam("cityCode", cityCode)
                            .queryParam("_type", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            return extractItems(response);
        } catch (WebClientResponseException e) {
            log.error("[BusClient] fetchTerminals HTTP 오류: {} {}", e.getStatusCode(), e.getMessage());
            throw new ExternalApiException("버스 터미널 조회 실패: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("[BusClient] fetchTerminals 실패: {}", e.getMessage());
            throw new ExternalApiException("버스 터미널 조회 실패", e);
        }
    }

    /**
     * 버스 등급 목록 조회
     */
    public List<Map<String, Object>> fetchBusGrades() {
        String key = getValidServiceKey();
        log.debug("[BusClient] fetchBusGrades");
        try {
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("apis.data.go.kr")
                            .path("/1613000/ExpBusInfoService2/getExpBusGradeList2")
                            .queryParam("serviceKey", key)
                            .queryParam("_type", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            return extractItems(response);
        } catch (WebClientResponseException e) {
            log.error("[BusClient] fetchBusGrades HTTP 오류: {} {}", e.getStatusCode(), e.getMessage());
            throw new ExternalApiException("버스 등급 조회 실패: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("[BusClient] fetchBusGrades 실패: {}", e.getMessage());
            throw new ExternalApiException("버스 등급 조회 실패", e);
        }
    }

    /**
     * 고속버스 시간표 조회
     * GET {baseUrl}/getExpBusTimeTable2?serviceKey=...&depTerminalId=...&arrTerminalId=...&depPlandTime=...&_type=json
     */
    public List<Map<String, Object>> fetchBusRoutes(String depTerminalId, String arrTerminalId, String date) {
        String key = getValidServiceKey();
        String depPlandTime = date != null ? date.replace("-", "") : "";
        log.info("[BusClient] fetchBusRoutes 호출: depTerminalId={} arrTerminalId={} date={}",
                depTerminalId, arrTerminalId, depPlandTime);
        try {
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("apis.data.go.kr")
                            .path("/1613000/ExpBusInfo/GetStrtpntAlocFndExpbusInfo")
                            .queryParam("serviceKey", key)
                            .queryParam("depTerminalId", depTerminalId)
                            .queryParam("arrTerminalId", arrTerminalId)
                            .queryParam("depPlandTime", depPlandTime)
                            .queryParam("_type", "json")
                            .queryParam("numOfRows", "30")
                            .queryParam("pageNo", "1")
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            List<Map<String, Object>> items = extractItems(response);
            log.info("[BusClient] 버스 API 응답: {}건", items.size());
            if (items.isEmpty()) {
                log.warn("[BusClient] 빈 결과 — 원본 응답: {}", response);
            }
            return items;
        } catch (WebClientResponseException e) {
            log.error("[BusClient] HTTP 오류: {} / body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalApiException("버스 시간표 조회 실패: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("[BusClient] fetchBusRoutes 실패: {}", e.getMessage());
            throw new ExternalApiException("버스 시간표 조회 실패", e);
        }
    }

    private String getValidServiceKey() {
        String key = properties.getBus().getServiceKey();
        if (key == null || key.isBlank() || key.equals("PLACEHOLDER")) {
            throw new ExternalApiException("버스 API 키가 설정되지 않았습니다");
        }
        return key;
    }

    /**
     * data.go.kr 공통 응답 구조에서 item 목록 추출
     * { "response": { "body": { "items": { "item": [...] } } } }
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(Map<String, Object> response) {
        if (response == null) return Collections.emptyList();
        try {
            Map<String, Object> resp = (Map<String, Object>) response.get("response");
            if (resp == null) return Collections.emptyList();
            Map<String, Object> body = (Map<String, Object>) resp.get("body");
            if (body == null) return Collections.emptyList();
            Object items = body.get("items");
            if (items instanceof Map) {
                Object item = ((Map<String, Object>) items).get("item");
                if (item instanceof List) return (List<Map<String, Object>>) item;
                if (item instanceof Map) return List.of((Map<String, Object>) item);
            }
        } catch (Exception e) {
            log.warn("[BusClient] 응답 파싱 실패: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
}
