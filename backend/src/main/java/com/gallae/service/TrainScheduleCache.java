package com.gallae.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 열차 시각표 캐시. API가 실패해도 이 시각표에 있는 열차만 보여준다(임의 시각 생성 안 함).
 * - KTX·일반열차: resources/data/KTX시간표.xlsx, 일반열차시간표.xlsx (코레일 공시 시각표, docs/ 원본 복사).
 *   엑셀을 못 읽으면 travelerTrainRunInfo2 API(어제 실제 운행 기록)로 대신한다.
 * - SRT: resources/data/srt_timetable.csv (docs/SRT 운행시각표('26.5.15.기준).hwp 에서 변환)
 *   + KTX시간표.xlsx 의 수서 정차 열차 중 CSV에 없는 열차.
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
    /** SRT trainNo → 정차역 목록 / 운행 요일(매일·금토일·월화수목) */
    private volatile Map<String, List<StopInfo>> srtStopsMap = Collections.emptyMap();
    private volatile Map<String, String> srtRunDays = Collections.emptyMap();
    /** 엑셀 시각표 열차 (KTX·SRT·ITX·무궁화). 같은 열차번호가 여러 노선 시트에 나올 수 있어 List. */
    private volatile List<ExcelTrain> excelTrains = Collections.emptyList();

    static final String[] TIMETABLE_FILES = {"data/KTX시간표.xlsx", "data/일반열차시간표.xlsx"};

    record ExcelTrain(String no, String type, String days, List<StopInfo> stops) {}

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    /** fromCity → toCity 구간의 실제 운행 1건 */
    public record Trip(String trainNo, String depStation, String arrStation,
                       String depTime, String arrTime, int minutes) {}

    @PostConstruct
    public void init() {
        loadSrtTimetable();
        loadExcelTimetables();
        if (!excelTrains.isEmpty()) return; // 엑셀 시각표가 있으면 어제 운행기록 API는 안 부른다
        try {
            loadSchedule();
        } catch (Exception e) {
            log.error("[TrainScheduleCache] 코레일 운행정보 로딩 실패 (KTX·일반열차 시각표 없음): {}", e.getMessage());
        }
    }

    // ─── public API ──────────────────────────────────────────────────────────

    /**
     * trainType 열차 중 fromCity → toCity 를 실제로 운행하는 열차 목록(출발시각순).
     * SRT는 date의 요일로 운행일을 거른다. 시각표에 없으면 빈 리스트.
     * shortcut: KTX·일반열차는 '어제' 운행 기록이라 요일별 임시열차 차이는 반영 안 됨, 정확도가 문제되면 날짜별 운행계획 API로 교체.
     */
    public List<Trip> findTrips(String trainType, String fromCity, String toCity, LocalDate date) {
        boolean srt = "SRT".equals(trainType);
        Map<String, Trip> trips = new LinkedHashMap<>(); // 열차번호+출발시각으로 중복 제거
        if (srt || excelTrains.isEmpty()) {
            for (Map.Entry<String, List<StopInfo>> entry : (srt ? srtStopsMap : trainStopsMap).entrySet()) {
                boolean match = srt ? runsOn(srtRunDays.get(entry.getKey()), date)
                                    : matchesTrainType(entry.getKey(), trainType);
                if (!match) continue;
                Trip trip = toTrip(entry.getKey(), entry.getValue(), fromCity, toCity);
                if (trip != null) trips.putIfAbsent(trip.trainNo() + trip.depTime(), trip);
            }
        }
        for (ExcelTrain t : excelTrains) {
            if (!t.type().equals(trainType) || !runsOn(t.days(), date)) continue;
            if (srt && srtStopsMap.containsKey(t.no())) continue; // SRT는 CSV 우선
            Trip trip = toTrip(t.no(), t.stops(), fromCity, toCity);
            if (trip != null) trips.putIfAbsent(trip.trainNo() + trip.depTime(), trip);
        }
        List<Trip> list = new ArrayList<>(trips.values());
        list.sort(Comparator.comparing(Trip::depTime));
        return list;
    }

    /** 시각표(엑셀·SRT CSV·어제 운행기록) 중 하나라도 로드됐는지. 전부 없을 때만 mock 으로 간다. */
    public boolean hasTimetable() {
        return !excelTrains.isEmpty() || !srtStopsMap.isEmpty() || !trainStopsMap.isEmpty();
    }

    /**
     * 비고의 운행요일(매일·금토일·월화수목·토일·월 …)에 date 요일이 들어 있으면 운행.
     * 요일 글자가 없는 비고("중앙선 경유" 등)는 매일 운행으로 본다.
     * shortcut: 공휴일은 요일 기준으로만 판단, 명절 특별수송 시각표가 필요하면 별도 파일 추가.
     */
    static boolean runsOn(String days, LocalDate date) {
        if (days == null || date == null || days.contains("매일")) return true;
        String week = "월화수목금토일";
        if (days.chars().noneMatch(ch -> week.indexOf(ch) >= 0)) return true;
        return days.indexOf(week.charAt(date.getDayOfWeek().getValue() - 1)) >= 0;
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

    /** SRT 시각표 CSV 로드. 형식: train_no,run_days,stop_seq,station,time (정차순) */
    void loadSrtTimetable() {
        Map<String, List<StopInfo>> stops = new HashMap<>();
        Map<String, String> days = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("data/srt_timetable.csv").getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] c = line.split(",");
                List<StopInfo> list = stops.computeIfAbsent(c[0], k -> new ArrayList<>());
                LocalTime time = LocalTime.parse(c[4]);
                LocalDateTime t = list.isEmpty() ? LocalDate.EPOCH.atTime(time)
                        : list.get(list.size() - 1).dptreDt.toLocalDate().atTime(time);
                if (!list.isEmpty() && t.isBefore(list.get(list.size() - 1).dptreDt)) t = t.plusDays(1); // 자정 넘김
                list.add(new StopInfo(c[3], null, t, t, Integer.parseInt(c[2])));
                days.put(c[0], c[1]);
            }
        } catch (Exception e) {
            log.error("[TrainScheduleCache] SRT 시각표 로딩 실패: {}", e.getMessage());
            return;
        }
        this.srtStopsMap = Collections.unmodifiableMap(stops);
        this.srtRunDays = Collections.unmodifiableMap(days);
        log.info("[TrainScheduleCache] SRT 시각표 {}개 열차 로드", stops.size());
    }

    /** KTX·일반열차 엑셀 시각표 로드. 파일이 없거나 깨지면 해당 파일만 건너뛴다. */
    void loadExcelTimetables() {
        List<ExcelTrain> all = new ArrayList<>();
        for (String path : TIMETABLE_FILES) {
            try (InputStream in = new ClassPathResource(path).getInputStream();
                 Workbook wb = new XSSFWorkbook(in)) {
                int before = all.size();
                for (Sheet sheet : wb) {
                    if (sheet.getSheetName().contains("보는방법")) continue; // 예시 시트
                    all.addAll(parseGrid(toGrid(sheet)));
                }
                log.info("[TrainScheduleCache] {} → {}개 열차", path, all.size() - before);
            } catch (Exception e) {
                log.error("[TrainScheduleCache] 엑셀 시각표 로딩 실패 {}: {}", path, e.getMessage());
            }
        }
        this.excelTrains = Collections.unmodifiableList(all);
    }

    /** 시트 → 2차원 값 배열. 문자열은 String, 숫자·시각은 Double(시각은 하루 비율, 0:00=0.0). */
    private static List<List<Object>> toGrid(Sheet sheet) {
        List<List<Object>> grid = new ArrayList<>();
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            List<Object> line = new ArrayList<>();
            if (row != null) {
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    CellType type = cell == null ? CellType.BLANK : cell.getCellType();
                    if (type == CellType.FORMULA) type = cell.getCachedFormulaResultType();
                    line.add(switch (type) {
                        case STRING -> cell.getStringCellValue();
                        case NUMERIC -> cell.getNumericCellValue();
                        default -> null;
                    });
                }
            }
            grid.add(line);
        }
        return grid;
    }

    /**
     * 시각표 그리드 파싱. '열차번호' 칸을 기준으로 두 가지 배치를 읽는다.
     * - KTX시간표: 행=열차, 열=역 ('열차번호' 오른쪽이 '편성')
     * - 일반열차시간표: 열=열차, 행=역 ('열차번호' 아래로 역 이름)
     * 시각 0:00 칸은 미정차.
     */
    static List<ExcelTrain> parseGrid(List<List<Object>> g) {
        List<ExcelTrain> trains = new ArrayList<>();
        for (int r = 0; r < g.size(); r++) {
            for (int c = 0; c < g.get(r).size(); c++) {
                if (!text(g, r, c).equals("열차번호")) continue;
                if (text(g, r, c + 1).equals("편성")) parseRowLayout(g, r, c, trains);
                else parseColumnLayout(g, r, c, trains);
            }
        }
        return trains;
    }

    private static void parseRowLayout(List<List<Object>> g, int r, int c, List<ExcelTrain> out) {
        List<String> stations = new ArrayList<>();
        int notesCol = -1;
        for (int k = c + 2; ; k++) {
            String h = text(g, r, k);
            if (h.isEmpty()) break;
            if (h.startsWith("비고")) { notesCol = k; break; }
            stations.add(h);
        }
        for (int i = r + 1; i < g.size(); i++) {
            if (text(g, i, c).equals("열차번호")) break; // 아래쪽 다른 표
            String no = trainNo(g, i, c);
            if (no == null) continue;
            List<double[]> times = new ArrayList<>();
            for (int j = 0; j < stations.size(); j++) times.add(new double[]{j, timeAt(g, i, c + 2 + j)});
            List<StopInfo> stops = toStops(stations, times);
            boolean suseo = stops.stream().anyMatch(s -> s.stnNm.equals("수서"));
            out.add(new ExcelTrain(no, suseo ? "SRT" : normalizeType(text(g, i, c + 1)),
                    notesCol < 0 ? null : text(g, i, notesCol), stops));
        }
    }

    private static void parseColumnLayout(List<List<Object>> g, int r, int c, List<ExcelTrain> out) {
        List<String> stations = new ArrayList<>();
        int notesRow = -1;
        for (int i = r + 1; i < g.size(); i++) {
            String s = text(g, i, c);
            if (s.isEmpty() || s.equals("종착역")) break;
            if (s.startsWith("비고")) { notesRow = i; break; }
            stations.add(s);
        }
        for (int k = c + 1; ; k++) {
            String no = trainNo(g, r, k);
            if (no == null) break;
            List<double[]> times = new ArrayList<>();
            for (int j = 0; j < stations.size(); j++) times.add(new double[]{j, timeAt(g, r + 1 + j, k)});
            out.add(new ExcelTrain(no, normalizeType(text(g, r - 1, k)),
                    notesRow < 0 ? null : text(g, notesRow, k), toStops(stations, times)));
        }
    }

    /** times: [역 인덱스, 하루 비율]. 0 이하는 미정차. 앞 역보다 이르면 자정을 넘긴 것으로 본다. */
    private static List<StopInfo> toStops(List<String> stations, List<double[]> times) {
        List<StopInfo> stops = new ArrayList<>();
        LocalDateTime prev = null;
        for (double[] t : times) {
            if (t[1] <= 0) continue;
            long minutes = Math.round((t[1] % 1) * 86400) / 60; // 초 단위(05:47:30)는 버림
            LocalDateTime at = LocalDate.EPOCH.atStartOfDay().plusMinutes(minutes);
            while (prev != null && at.isBefore(prev)) at = at.plusDays(1);
            stops.add(new StopInfo(stations.get((int) t[0]), null, at, at, stops.size() + 1));
            prev = at;
        }
        return stops;
    }

    private static String normalizeType(String raw) {
        if (raw.contains("KTX")) return "KTX";
        if (raw.contains("마음")) return "ITX-마음";
        if (raw.contains("새마을")) return "ITX-새마을";
        if (raw.contains("무궁화") || raw.contains("누리로")) return "무궁화호";
        return raw;
    }

    /** 공백(\u00a0 포함)을 지운 문자열. 숫자·빈칸은 "". */
    private static String text(List<List<Object>> g, int r, int c) {
        Object v = (r >= 0 && r < g.size() && c >= 0 && c < g.get(r).size()) ? g.get(r).get(c) : null;
        return v instanceof String s ? s.replaceAll("[\\s\u00a0]", "") : "";
    }

    private static double timeAt(List<List<Object>> g, int r, int c) {
        Object v = (r < g.size() && c < g.get(r).size()) ? g.get(r).get(c) : null;
        return v instanceof Double d ? d : 0;
    }

    /** 열차번호 칸: 1 이상 정수(숫자 또는 숫자 문자열)만 인정. */
    private static String trainNo(List<List<Object>> g, int r, int c) {
        Object v = (r < g.size() && c < g.get(r).size()) ? g.get(r).get(c) : null;
        if (v instanceof Double d && d >= 1 && d == Math.floor(d)) return String.valueOf(d.longValue());
        String s = text(g, r, c);
        return s.matches("\\d+") ? s.replaceAll("^0+", "") : null;
    }

    // ─── 계산 ────────────────────────────────────────────────────────────────

    private Trip toTrip(String trainNo, List<StopInfo> stops, String fromCity, String toCity) {
        StopInfo fromStop = null;
        for (StopInfo s : stops) {
            if (fromStop == null) {
                if (cityMatches(s.stnNm, fromCity)) fromStop = s;
            } else if (cityMatches(s.stnNm, toCity)) {
                LocalDateTime dep = fromStop.dptreDt != null ? fromStop.dptreDt : fromStop.arvlDt;
                LocalDateTime arr = s.arvlDt != null ? s.arvlDt : s.dptreDt;
                if (dep == null || arr == null) return null;
                int minutes = (int) Duration.between(dep, arr).toMinutes();
                return minutes > 0
                        ? new Trip(trainNo.replaceAll("^0+", ""), fromStop.stnNm, s.stnNm,
                                   dep.format(HHMM), arr.format(HHMM), minutes)
                        : null;
            }
        }
        return null;
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
        if (c.equals("광주송정") && s.equals("광주")) return false;  // 광주 검색은 광주송정(KTX)도 포함
        if (c.equals("서울") && (s.equals("용산") || s.equals("청량리"))) return true; // 호남·전라·중앙선 서울 출발역
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
