package com.gallae.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * travelerTrainRunInfo2 API(어제 실제 운행 기록)를 애플리케이션 시작 시 캐싱.
 * 모든 중간역 정차 시각을 실측값으로 보유하므로, SEGMENT_DURATION 정적 테이블보다 정확하다.
 *
 * 보존기간: 여객열차 운행정보 = 3개월 ~ 1일 전 → 어제 날짜 항상 사용 가능.
 */
@Slf4j
@Service
public class TrainScheduleCache {

    private static final String BASE_URL =
            "https://apis.data.go.kr/B551457/run/v2/travelerTrainRunInfo2";
    private static final int PAGE_SIZE = 1000;

    private final com.gallae.config.ExternalApiProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public TrainScheduleCache(com.gallae.config.ExternalApiProperties properties) {
        this.properties = properties;
    }

    /** trainNo → 순서대로 정렬된 정차역 목록 */
    private volatile Map<String, List<StopInfo>> trainStopsMap = Collections.emptyMap();

    @PostConstruct
    public void init() {
        try {
            loadSchedule();
        } catch (Exception e) {
            log.error("[TrainScheduleCache] 초기화 실패 (SEGMENT_DURATION fallback 사용): {}", e.getMessage());
        }
    }

    // ─── public API ──────────────────────────────────────────────────────────

    /**
     * 주어진 trainType의 열차 중 fromCity → toCity 구간 최소 소요시간(분) 반환.
     * 데이터 없으면 -1.
     */
    public int findMinDuration(String trainType, String fromCity, String toCity) {
        if (trainStopsMap.isEmpty()) return -1;
        int min = Integer.MAX_VALUE;
        for (Map.Entry<String, List<StopInfo>> entry : trainStopsMap.entrySet()) {
            if (!matchesTrainType(entry.getKey(), trainType)) continue;
            int dur = calcDuration(entry.getValue(), fromCity, toCity);
            if (dur > 0 && dur < min) min = dur;
        }
        return min == Integer.MAX_VALUE ? -1 : min;
    }

    /** 캐시 로드 여부 */
    public boolean isLoaded() {
        return !trainStopsMap.isEmpty();
    }

    // ─── 로딩 ────────────────────────────────────────────────────────────────

    void loadSchedule() throws Exception {
        String yesterday = LocalDate.now().minusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        log.info("[TrainScheduleCache] {} 운행정보 로딩 시작", yesterday);

        // 1페이지로 totalCount 확인
        Map<String, Object> first = fetchPage(yesterday, 1, 1);
        int totalCount = extractTotalCount(first);
        log.info("[TrainScheduleCache] totalCount={}", totalCount);
        if (totalCount == 0) {
            log.warn("[TrainScheduleCache] 데이터 없음");
            return;
        }

        // 전체 페이지 순회
        int totalPages = (totalCount + PAGE_SIZE - 1) / PAGE_SIZE;
        List<Map<String, Object>> allItems = new ArrayList<>(totalCount);
        for (int page = 1; page <= totalPages; page++) {
            Map<String, Object> pageData = fetchPage(yesterday, page, PAGE_SIZE);
            allItems.addAll(extractItems(pageData));
            log.debug("[TrainScheduleCache] 페이지 {}/{} 로드 (누적 {}건)", page, totalPages, allItems.size());
        }

        // trainNo 별 그룹화 + 순서 정렬
        Map<String, List<StopInfo>> map = new ConcurrentHashMap<>();
        for (Map<String, Object> item : allItems) {
            String trainNo = str(item, "trn_no");
            if (trainNo == null || trainNo.isBlank()) continue;
            StopInfo stop = parseStop(item);
            if (stop != null) map.computeIfAbsent(trainNo, k -> new ArrayList<>()).add(stop);
        }
        map.values().forEach(list -> list.sort(Comparator.comparingInt(s -> s.runSn)));
        this.trainStopsMap = Collections.unmodifiableMap(map);
        log.info("[TrainScheduleCache] 완료: {}개 열차, {}개 정차역", map.size(), allItems.size());
    }

    // ─── 계산 ────────────────────────────────────────────────────────────────

    private int calcDuration(List<StopInfo> stops, String fromCity, String toCity) {
        StopInfo fromStop = null;
        for (StopInfo s : stops) {
            if (fromStop == null) {
                if (cityMatches(s.stnNm, fromCity)) fromStop = s;
            } else {
                if (cityMatches(s.stnNm, toCity)) {
                    LocalDateTime dep = fromStop.dptreDt != null ? fromStop.dptreDt : fromStop.arvlDt;
                    LocalDateTime arr = s.arvlDt != null ? s.arvlDt : s.dptreDt;
                    if (dep == null || arr == null) return -1;
                    int minutes = (int) Duration.between(dep, arr).toMinutes();
                    return minutes > 0 ? minutes : -1;
                }
            }
        }
        return -1;
    }

    // ─── 매칭 ────────────────────────────────────────────────────────────────

    private boolean matchesTrainType(String trainNoRaw, String trainType) {
        int no;
        try { no = Integer.parseInt(trainNoRaw.replaceAll("[^0-9]", "")); }
        catch (Exception e) { return false; }
        return switch (trainType) {
            case "KTX", "KTX-산천", "KTX-이음" ->
                    (no >= 1 && no <= 199) || (no >= 401 && no <= 499)
                    || (no >= 4501 && no <= 4599) || (no >= 5001 && no <= 5099);
            case "ITX-새마을" -> no >= 1001 && no <= 1099;
            case "ITX-마음"   -> no >= 1101 && no <= 1149;
            case "무궁화호", "누리로" -> no >= 1150 && no <= 2199;
            default -> false;
        };
    }

    /**
     * 역명 ↔ 도시명 매칭.
     * 명시적으로 다른 역을 서로 매칭하지 않도록 예외 처리 포함.
     */
    private boolean cityMatches(String stnNm, String city) {
        if (stnNm == null || city == null) return false;
        String s = stnNm.replace("역", "").trim();
        String c = city.replace("역", "").replace("터미널", "").trim();
        // 명백히 다른 역은 contains 로 오매칭되지 않도록 차단
        if (c.equals("천안") && s.equals("천안아산")) return false;
        if (c.equals("천안아산") && s.equals("천안")) return false;
        if (c.equals("광주") && s.equals("광주송정")) return false;
        if (c.equals("광주송정") && s.equals("광주")) return false;
        return s.equals(c) || s.contains(c) || c.contains(s);
    }

    // ─── 파싱 ────────────────────────────────────────────────────────────────

    private StopInfo parseStop(Map<String, Object> item) {
        try {
            String stnNm   = str(item, "stn_nm");
            String stnCd   = str(item, "stn_cd");
            int    runSn   = parseInt(item, "trn_run_sn");
            LocalDateTime arvl  = parseDateTime(str(item, "trn_arvl_dt"));
            LocalDateTime dptre = parseDateTime(str(item, "trn_dptre_dt"));
            return new StopInfo(stnNm, stnCd, arvl, dptre, runSn);
        } catch (Exception e) {
            return null;
        }
    }

    private static final List<DateTimeFormatter> DT_FMTS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"),  // v2: "2026-06-21 08:30:00.0"
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),    // "2026-06-21 08:30:00"
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),          // "20260621083000"
            DateTimeFormatter.ofPattern("yyyyMMddHHmm")             // "202606210830"
    );

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.equalsIgnoreCase("null") || raw.isBlank()) return null;
        String s = raw.trim();
        for (DateTimeFormatter fmt : DT_FMTS) {
            try { return LocalDateTime.parse(s, fmt); }
            catch (Exception ignored) {}
        }
        return null;
    }

    // ─── HTTP + JSON ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchPage(String runYmd, int pageNo, int numOfRows) throws Exception {
        String url = BASE_URL
                + "?serviceKey=" + properties.getKorail().getServiceKey()
                + "&pageNo=" + pageNo
                + "&numOfRows=" + numOfRows
                + "&returnType=JSON"
                + "&cond%5Brun_ymd%3A%3AEQ%5D=" + runYmd;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET().build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new RuntimeException("HTTP " + res.statusCode());
        return objectMapper.readValue(res.body(), new TypeReference<>() {});
    }

    @SuppressWarnings("unchecked")
    private int extractTotalCount(Map<String, Object> response) {
        try {
            // v2 API: { "totalCount": N, "data": [...] }
            Object tc = response.get("totalCount");
            if (tc instanceof Number) return ((Number) tc).intValue();
            if (tc != null) return Integer.parseInt(tc.toString());
            // 구형 API: { "response": { "body": { "totalCount": N } } }
            Map<String, Object> resp = (Map<String, Object>) response.get("response");
            Map<String, Object> body = (Map<String, Object>) resp.get("body");
            tc = body.get("totalCount");
            return tc instanceof Number ? ((Number) tc).intValue() : Integer.parseInt(tc.toString());
        } catch (Exception e) { return 0; }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(Map<String, Object> response) {
        try {
            // v2 API: { "data": [...] }
            Object data = response.get("data");
            if (data instanceof List) return (List<Map<String, Object>>) data;
            // 구형 API: { "response": { "body": { "items": { "item": [...] } } } }
            Map<String, Object> resp  = (Map<String, Object>) response.get("response");
            Map<String, Object> body  = (Map<String, Object>) resp.get("body");
            Map<String, Object> items = (Map<String, Object>) body.get("items");
            Object item = items.get("item");
            if (item instanceof List) return (List<Map<String, Object>>) item;
            if (item instanceof Map)  return List.of((Map<String, Object>) item);
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    // ─── 유틸 ────────────────────────────────────────────────────────────────

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : v.toString();
    }

    private int parseInt(Map<String, Object> m, String key) {
        try { return Integer.parseInt(str(m, key)); }
        catch (Exception e) { return 0; }
    }

    // ─── 내부 DTO ────────────────────────────────────────────────────────────

    static class StopInfo {
        final String stnNm;
        final String stnCd;
        final LocalDateTime arvlDt;
        final LocalDateTime dptreDt;
        final int runSn;

        StopInfo(String stnNm, String stnCd, LocalDateTime arvlDt, LocalDateTime dptreDt, int runSn) {
            this.stnNm   = stnNm;
            this.stnCd   = stnCd;
            this.arvlDt  = arvlDt;
            this.dptreDt = dptreDt;
            this.runSn   = runSn;
        }
    }
}
