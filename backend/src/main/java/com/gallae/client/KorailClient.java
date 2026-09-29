package com.gallae.client;

import com.gallae.config.ExternalApiProperties;
import com.gallae.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class KorailClient {

    private final WebClient.Builder webClientBuilder;
    private final ExternalApiProperties properties;

    /**
     * 열차 노선 코드 목록 조회
     * GET {baseUrl}/codes2?serviceKey=...&_type=json
     */
    public List<Map<String, Object>> fetchTrainCodes() {
        String key = getValidServiceKey();
        log.debug("[KorailClient] fetchTrainCodes");
        try {
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("apis.data.go.kr")
                            .path("/B551457/run/v2/codes2")
                            .queryParam("serviceKey", key)
                            .queryParam("_type", "json")
                            .queryParam("numOfRows", "100")
                            .queryParam("pageNo", "1")
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            return extractKorailItems(response);
        } catch (WebClientResponseException e) {
            log.error("[KorailClient] fetchTrainCodes HTTP 오류: {} {}", e.getStatusCode(), e.getMessage());
            throw new ExternalApiException("코레일 코드 조회 실패: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("[KorailClient] fetchTrainCodes 실패: {}", e.getMessage());
            throw new ExternalApiException("코레일 코드 조회 실패", e);
        }
    }

    /**
     * 열차 운행 계획 조회
     * GET {baseUrl}/trainLineList?serviceKey=...&depPlaceId=...&arrPlaceId=...&depPlandTime=...&_type=json
     */
    public List<Map<String, Object>> fetchTrainRunPlan(String date, String depCode, String arrCode) {
        String key = getValidServiceKey();
        String runYmd = date != null ? date.replace("-", "") : "";
        log.info("[KorailClient] fetchTrainRunPlan 호출: date={} depCode={} arrCode={}", date, depCode, arrCode);
        try {
            // cond[] 파라미터: Spring UriBuilder가 [] :: 를 이중 인코딩하므로 raw URI로 직접 전달
            // data.go.kr 서버는 %5B, %5D, %3A%3A 를 디코딩해서 cond[run_ymd::GTE] 로 인식
            String rawUri = "https://apis.data.go.kr/B551457/run/v2/travelerTrainRunPlan2"
                    + "?serviceKey=" + key
                    + "&pageNo=1&numOfRows=100&returnType=JSON"
                    + "&cond%5Brun_ymd%3A%3AGTE%5D=" + runYmd
                    + "&cond%5Brun_ymd%3A%3ALTE%5D=" + runYmd
                    + "&cond%5Bdptre_stn_cd%3A%3AEQ%5D=" + depCode
                    + "&cond%5Barvl_stn_cd%3A%3AEQ%5D=" + arrCode;
            log.info("[KorailClient] 요청 URI: {}", rawUri.replaceAll("serviceKey=[^&]+", "serviceKey=MASKED"));
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(URI.create(rawUri))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            List<Map<String, Object>> items = extractKorailItems(response);
            log.info("[KorailClient] 코레일 API 응답: {}건", items.size());
            if (items.isEmpty()) log.warn("[KorailClient] 빈 결과 — 원본 응답: {}", response);
            return items;
        } catch (WebClientResponseException e) {
            log.error("[KorailClient] HTTP 오류: {} / body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalApiException("코레일 열차 조회 실패: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("[KorailClient] fetchTrainRunPlan 실패: {}", e.getMessage());
            throw new ExternalApiException("코레일 열차 조회 실패", e);
        }
    }

    /**
     * 열차 운행 정보 상세 조회
     */
    public List<Map<String, Object>> fetchTrainRunInfo(String date, String trainNo) {
        String key = getValidServiceKey();
        log.debug("[KorailClient] fetchTrainRunInfo: {} {}", date, trainNo);
        try {
            String depPlandTime = date != null ? date.replace("-", "") : "";
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("apis.data.go.kr")
                            .path("/B551457/run/v2/trainRunOrder")
                            .queryParam("serviceKey", key)
                            .queryParam("trainNo", trainNo)
                            .queryParam("depPlandTime", depPlandTime)
                            .queryParam("_type", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            return extractKorailItems(response);
        } catch (WebClientResponseException e) {
            log.error("[KorailClient] fetchTrainRunInfo HTTP 오류: {} {}", e.getStatusCode(), e.getMessage());
            throw new ExternalApiException("코레일 운행 정보 조회 실패: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("[KorailClient] fetchTrainRunInfo 실패: {}", e.getMessage());
            throw new ExternalApiException("코레일 운행 정보 조회 실패", e);
        }
    }

    private String getValidServiceKey() {
        String key = properties.getKorail().getServiceKey();
        if (key == null || key.isBlank() || key.equals("PLACEHOLDER")) {
            throw new ExternalApiException("코레일 API 키가 설정되지 않았습니다");
        }
        return key;
    }

    /**
     * travelerTrainRunPlan2 응답 구조 추출
     * { "currentCount": N, "data": [...], "totalCount": N }
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractKorailItems(Map<String, Object> response) {
        if (response == null) return Collections.emptyList();
        try {
            // v2 list API 응답: { "data": [...] }
            Object data = response.get("data");
            if (data instanceof List) return (List<Map<String, Object>>) data;

            // 구형 응답 구조도 fallback 처리
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
            log.warn("[KorailClient] 응답 파싱 실패: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
}
