package com.gallae.service;

import com.gallae.client.*;
import com.gallae.dto.*;
import com.gallae.exception.ExternalApiException;
import com.gallae.util.FallbackFareLoader;
import com.gallae.util.LocationCodeMapper;
import com.gallae.util.MockDataProvider;
import com.gallae.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    /**
     * 버스 fallback 테이블. [소요분, 우등운임(원)]
     * API 실패 시 구간별 실제 데이터 사용 (코버스/티머니 공시 기준).
     * 단방향 등록, 조회 시 역방향도 시도.
     */
    private static final Map<String, long[]> BUS_FALLBACK;
    static {
        Map<String, long[]> b = new LinkedHashMap<>();
        // [소요분, 우등운임]
        b.put("서울|부산",   new long[]{270, 31600});
        b.put("서울|울산",   new long[]{250, 27100});
        b.put("서울|경주",   new long[]{260, 29100});
        b.put("서울|대구",   new long[]{180, 24000});
        b.put("서울|동대구", new long[]{180, 24000});
        b.put("서울|광주",   new long[]{220, 20500});
        b.put("서울|대전",   new long[]{ 95, 10800});
        b.put("서울|전주",   new long[]{145, 16400});
        b.put("서울|익산",   new long[]{135, 15200});
        b.put("서울|목포",   new long[]{265, 27700});
        b.put("서울|여수",   new long[]{290, 27800});
        b.put("서울|진주",   new long[]{260, 26600});
        b.put("서울|강릉",   new long[]{160, 20400});
        b.put("서울|원주",   new long[]{ 90, 11800});
        b.put("서울|춘천",   new long[]{ 90, 10700});
        b.put("서울|포항",   new long[]{240, 28600});
        b.put("서울|안동",   new long[]{180, 21000});
        BUS_FALLBACK = Collections.unmodifiableMap(b);
    }

    /**
     * 자가용 fallback 테이블. [소요분, 거리km*10, 통행료(원), 유류비(원)]
     * 경부고속도로·서해안고속도로 등 실측 기준.
     */
    private static final Map<String, long[]> CAR_FALLBACK;
    static {
        Map<String, long[]> c = new LinkedHashMap<>();
        // [소요분, 거리km*10(소수 1자리), 통행료, 유류비]
        c.put("서울|부산",   new long[]{240, 3250, 28100, 46000});
        c.put("서울|울산",   new long[]{215, 3050, 25600, 43000});
        c.put("서울|경주",   new long[]{220, 3170, 26900, 45000});
        c.put("서울|대구",   new long[]{170, 2840, 22700, 40000});
        c.put("서울|동대구", new long[]{170, 2840, 22700, 40000});
        c.put("서울|광주",   new long[]{200, 2900, 19300, 41000});
        c.put("서울|대전",   new long[]{ 95, 1660,  8700, 23000});
        c.put("서울|전주",   new long[]{155, 2190, 14200, 31000});
        c.put("서울|익산",   new long[]{145, 2120, 13600, 30000});
        c.put("서울|목포",   new long[]{250, 3340, 22600, 47000});
        c.put("서울|여수",   new long[]{270, 3490, 25600, 49000});
        c.put("서울|진주",   new long[]{245, 3300, 26500, 47000});
        c.put("서울|강릉",   new long[]{160, 2290, 12800, 32000});
        c.put("서울|원주",   new long[]{ 90, 1520,  5600, 22000});
        c.put("서울|춘천",   new long[]{ 75, 1200,  4200, 17000});
        c.put("서울|포항",   new long[]{230, 3140, 25200, 44000});
        c.put("서울|안동",   new long[]{180, 2440, 18100, 35000});
        CAR_FALLBACK = Collections.unmodifiableMap(c);
    }

    private final KorailClient korailClient;
    private final BusClient busClient;
    private final KakaoNaviClient kakaoNaviClient;
    private final SrtClient srtClient;
    private final MockDataProvider mockDataProvider;
    private final FallbackFareLoader fallbackFareLoader;
    private final LocationCodeMapper locationCodeMapper;
    private final TrainScheduleCache trainScheduleCache;

    public RouteSearchResponse search(String from, String to, String date, String time) {
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            throw new IllegalArgumentException("출발지와 도착지를 입력해주세요.");
        }

        log.info("[SearchService] 검색 시작: {} -> {} / {} {}", from, to, date, time);

        try {
            return buildSearchResponse(from, to, date, time);
        } catch (Exception e) {
            log.warn("[SearchService] 실제 API 실패, mock 데이터 반환: {}", e.getMessage());
            return mockDataProvider.getMockSearchResult(from, to, date, time);
        }
    }

    private RouteSearchResponse buildSearchResponse(String from, String to, String date, String time) {
        List<RouteResult> results = new ArrayList<>();
        LocalDate searchDate = toLocalDate(date);

        // 1. 열차 스케줄 (코레일 API) — 도시명 → 역 코드 변환 후 호출
        CompletableFuture<List<RouteResult>> trainFuture = CompletableFuture.supplyAsync(() -> {
            String depCode = locationCodeMapper.toKorailStationCode(from);
            String arrCode = locationCodeMapper.toKorailStationCode(to);

            List<RouteResult> korailResults = new ArrayList<>();
            if (depCode != null && arrCode != null) {
                try {
                    // ① 기본 조회: 출발지 역 코드 기준
                    List<Map<String, Object>> trainData = korailClient.fetchTrainRunPlan(date, depCode, arrCode);
                    korailResults.addAll(convertTrainDataToRouteResults(trainData, from, to));

                    // ② 호남선 보조 조회: ITX-새마을·무궁화호는 용산역(3900024) 출발
                    //    서울역(3900023) 코드로만 조회하면 용산출발 열차가 누락됨
                    String altDepCode = getAltDepCode(from, to);
                    if (altDepCode != null) {
                        try {
                            List<Map<String, Object>> altData = korailClient.fetchTrainRunPlan(date, altDepCode, arrCode);
                            List<RouteResult> altResults = convertTrainDataToRouteResults(altData, "용산", to);
                            // 중복 열차번호 제거 후 합산
                            Set<String> existingNames = korailResults.stream().map(RouteResult::getName).collect(Collectors.toSet());
                            altResults.stream().filter(r -> !existingNames.contains(r.getName())).forEach(korailResults::add);
                            log.info("[SearchService] 용산 출발 추가 조회: {}건", altResults.size());
                        } catch (ExternalApiException e) {
                            log.warn("[SearchService] 용산 출발 조회 실패: {}", e.getMessage());
                        }
                    }

                    // ③ 역방향 호남선: ITX-새마을·무궁화호가 용산역 도착하는 경우 보완
                    String altArrCode = getAltArrCode(from, to);
                    if (altArrCode != null) {
                        try {
                            List<Map<String, Object>> altData = korailClient.fetchTrainRunPlan(date, depCode, altArrCode);
                            List<RouteResult> altResults = convertTrainDataToRouteResults(altData, from, "용산");
                            Set<String> existingNames = korailResults.stream().map(RouteResult::getName).collect(Collectors.toSet());
                            altResults.stream().filter(r -> !existingNames.contains(r.getName())).forEach(korailResults::add);
                            log.info("[SearchService] 용산 도착 추가 조회: {}건", altResults.size());
                        } catch (ExternalApiException e) {
                            log.warn("[SearchService] 용산 도착 조회 실패: {}", e.getMessage());
                        }
                    }

                    if (time != null && !time.isBlank()) {
                        int fromMin = parseTimeToMinutes(time);
                        korailResults = korailResults.stream()
                            .filter(r -> r.getDepartureTime() == null || parseTimeToMinutes(r.getDepartureTime()) >= fromMin)
                            .collect(Collectors.toList());
                    }
                    log.info("[SearchService] 코레일 API 데이터 총 {}건", korailResults.size());
                } catch (ExternalApiException e) {
                    log.warn("[SearchService] 코레일 API 실패: {}", e.getMessage());
                }
            } else {
                log.info("[SearchService] 코레일 역 코드 없음({}/{})", from, to);
            }

            // 코레일 API에서 반환하지 않은 열차 종류 보충 (KTX 포함)
            Set<String> foundTypes = korailResults.stream().map(RouteResult::getType).collect(Collectors.toSet());
            List<RouteResult> all = new ArrayList<>(korailResults);
            all.addAll(getSupplementalTrainResults(from, to, time, searchDate, foundTypes));
            // API가 "KTX-산천 303", 시각표가 "KTX 303"처럼 이름만 다른 같은 열차 → 열차번호로 중복 제거(API 우선)
            Set<String> seen = new HashSet<>();
            all.removeIf(r -> {
                String no = r.getName() == null ? "" : r.getName().replaceAll(".*\\s", "");
                String type = r.getType() != null && r.getType().startsWith("KTX") ? "KTX" : r.getType();
                return no.matches("\\d+") && !seen.add(type + " " + no);
            });
            log.info("[SearchService] 열차 최종 {}건 (코레일 API {}건 + 시각표 {}건)",
                    all.size(), korailResults.size(), all.size() - korailResults.size());
            if (all.isEmpty() && !trainScheduleCache.hasTimetable()) return mockTrainResults(from, to, date, time);
            return all;
        });

        // 2. 고속버스 (TAGO API) — 도시명 → 터미널 ID 변환 후 호출
        CompletableFuture<List<RouteResult>> busFuture = CompletableFuture.supplyAsync(() -> {
            String depTerminalId = locationCodeMapper.toBusTerminalId(from);
            String arrTerminalId = locationCodeMapper.toBusTerminalId(to);
            if (depTerminalId == null || arrTerminalId == null) {
                log.info("[SearchService] 버스 터미널 코드 없음({}/{}), 기본 데이터 사용", from, to);
                return getBusFallbackResults(from, to, time);
            }
            try {
                List<Map<String, Object>> busData = busClient.fetchBusRoutes(depTerminalId, arrTerminalId, date);
                List<RouteResult> busResults = convertBusDataToRouteResults(busData, from, to);

                if (time != null && !time.isBlank()) {
                    int fromMin = parseTimeToMinutes(time);
                    busResults = busResults.stream()
                        .filter(r -> r.getDepartureTime() == null || parseTimeToMinutes(r.getDepartureTime()) >= fromMin)
                        .collect(Collectors.toList());
                }
                if (!busResults.isEmpty()) {
                    log.info("[SearchService] 버스 API 데이터 {}건 사용", busResults.size());
                    return busResults;
                }
                log.info("[SearchService] 버스 API 결과 없음, 기본 데이터 사용");
                return getBusFallbackResults(from, to, time);
            } catch (ExternalApiException e) {
                log.warn("[SearchService] 버스 API 실패, 기본 데이터 사용: {}", e.getMessage());
                return getBusFallbackResults(from, to, time);
            }
        });

        // 3. 자가용 (카카오 API) — 도시명 → 좌표 변환 후 호출
        CompletableFuture<RouteResult> carFuture = CompletableFuture.supplyAsync(() -> {
            String originCoords = locationCodeMapper.toCoordinates(from);
            String destCoords = locationCodeMapper.toCoordinates(to);
            if (originCoords == null || destCoords == null) {
                log.info("[SearchService] 카카오 좌표 없음({}/{}), 기본 데이터 사용", from, to);
                return getCarFallbackResult(from, to);
            }
            try {
                Map<String, Object> naviData = kakaoNaviClient.fetchDirections(originCoords, destCoords);
                RouteResult carResult = convertNaviDataToRouteResult(naviData, from, to);
                if (carResult != null) {
                    log.info("[SearchService] 카카오 API 데이터 사용");
                    return carResult;
                }
                return getCarFallbackResult(from, to);
            } catch (ExternalApiException e) {
                log.warn("[SearchService] 카카오 API 실패, 기본 데이터 사용: {}", e.getMessage());
                return getCarFallbackResult(from, to);
            }
        });

        // 결과 합치기 (각 Future에 10초 타임아웃)
        try {
            results.addAll(trainFuture.get(10, TimeUnit.SECONDS));
        } catch (TimeoutException e) {
            log.warn("[SearchService] 열차 API 타임아웃 → 시각표 fallback");
            trainFuture.cancel(true);
            results.addAll(trainFallbackResults(from, to, date, time, searchDate));
        } catch (Exception e) {
            log.warn("[SearchService] 열차 API 오류 → 시각표 fallback: {}", e.getMessage());
            results.addAll(trainFallbackResults(from, to, date, time, searchDate));
        }
        try {
            results.addAll(busFuture.get(10, TimeUnit.SECONDS));
        } catch (TimeoutException e) {
            log.warn("[SearchService] 버스 API 타임아웃 → fallback");
            busFuture.cancel(true);
            results.addAll(getBusFallbackResults(from, to, time));
        } catch (Exception e) {
            log.warn("[SearchService] 버스 API 오류 → fallback: {}", e.getMessage());
            results.addAll(getBusFallbackResults(from, to, time));
        }
        try {
            RouteResult carResult = carFuture.get(10, TimeUnit.SECONDS);
            if (carResult != null) results.add(carResult);
        } catch (TimeoutException e) {
            log.warn("[SearchService] 카카오 API 타임아웃 → fallback");
            carFuture.cancel(true);
            results.add(getCarFallbackResult(from, to));
        } catch (Exception e) {
            log.warn("[SearchService] 카카오 API 오류 → fallback: {}", e.getMessage());
            results.add(getCarFallbackResult(from, to));
        }

        if (results.isEmpty()) {
            return mockDataProvider.getMockSearchResult(from, to, date, time);
        }

        return RouteSearchResponse.builder()
                .from(from).to(to).date(date).time(time)
                .results(results)
                .summary(buildSummary(results))
                .disclaimer("표시된 가격은 참고용이며 실제 가격은 각 예매 사이트에서 확인하세요.")
                .build();
    }

    // ─── 데이터 변환 ─────────────────────────────────────────────────────────

    /**
     * 코레일 API 응답 → RouteResult 변환
     * 응답 필드: trainNo, depPlaceNm(또는 depPlaceName), arrPlaceNm(또는 arrPlaceName),
     *            depPlandTime, arrPlandTime, trainGradeNm, adultCharge 등
     */
    private List<RouteResult> convertTrainDataToRouteResults(List<Map<String, Object>> items, String from, String to) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        // 첫 번째 아이템의 실제 필드명 확인용 로그 (한 번만)
        log.info("[SearchService] 코레일 응답 샘플 필드: {}", items.get(0).keySet());
        log.info("[SearchService] 코레일 응답 샘플 값: {}", items.get(0));
        List<RouteResult> results = new ArrayList<>();
        for (Map<String, Object> item : items) {
            try {
                // travelerTrainRunPlan2 v2 API 필드명
                // ※ 이 API는 열차종류명(trn_clsf_nm)을 제공하지 않으므로 열차번호로 추론
                String trainNo    = getStringFirstMatch(item, "",
                        "trn_no", "trnNo", "train_no", "trainNo");
                String trainGrade = getStringFirstMatch(item, null,
                        "trn_clsf_nm", "trnClsfNm", "train_grade_nm", "trainGradeNm", "trn_gbn_nm");
                if (trainGrade == null || trainGrade.isBlank()) {
                    trainGrade = inferTrainTypeFromNo(trainNo);
                }
                String depTime    = formatTime(getStringFirstMatch(item, "",
                        "trn_plan_dptre_dt",                   // ← 실제 필드명 (진단 로그 확인)
                        "dptre_plandtime", "dpt_tm", "dptTm",
                        "depPlandTime", "dep_tm", "departure_time"));
                String arrTime    = formatTime(getStringFirstMatch(item, "",
                        "trn_plan_arvl_dt",                    // ← 실제 필드명 (진단 로그 확인)
                        "arvl_plandtime", "arvl_tm", "arvlTm",
                        "arrPlandTime", "arr_tm", "arrival_time"));
                String depName    = getStringFirstMatch(item, from + "역",
                        "dptre_stn_nm", "dptRsStnNm", "dpt_rs_stn_nm", // 출발역명
                        "depPlaceNm", "depPlaceName", "dep_stn_nm");
                String arrName    = getStringFirstMatch(item, to + "역",
                        "arvl_stn_nm", "arvlRsStnNm", "arvl_rs_stn_nm", // 도착역명
                        "arrPlaceNm", "arrPlaceName", "arr_stn_nm");
                long   fare       = parseLongFirstMatch(item, 0L,
                        "adultCharge", "adult_charge", "chargeStd", "fare", "psgTyp1Amt");
                // travelerTrainRunPlan API는 요금 미제공 → CSV fallback
                if (fare == 0L) fare = lookupFareFromCsv(trainGrade, from, to);
                int    duration   = calcDurationMinutes(depTime, arrTime);
                String bookingUrl = trainGrade != null && trainGrade.equals("SRT")
                        ? "https://etk.srail.kr"
                        : "https://www.letskorail.com";

                // 열차번호 앞의 0 제거: "00101" → "101"
                String displayNo = trainNo.replaceAll("^0+", "");
                results.add(RouteResult.builder()
                        .type(trainGrade)
                        .name(trainGrade + (displayNo.isBlank() ? "" : " " + displayNo))
                        .departureName(depName)
                        .arrivalName(arrName)
                        .departureTime(depTime)
                        .arrivalTime(arrTime)
                        .durationMinutes(duration > 0 ? duration : null)
                        .fare(fare > 0 ? fare : null)
                        .recommendTags(new ArrayList<>())
                        .bookingUrl(bookingUrl)
                        .build());
            } catch (Exception e) {
                log.warn("[SearchService] 열차 데이터 변환 실패: {}", e.getMessage());
            }
        }
        return results;
    }

    /**
     * 고속버스 API 응답 → RouteResult 변환
     * 응답 필드: depTerminalNm(또는 depTerminalName), arrTerminalNm, depPlandTime, arrPlandTime, gradeNm, charge 등
     */
    private List<RouteResult> convertBusDataToRouteResults(List<Map<String, Object>> items, String from, String to) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        log.info("[SearchService] 버스 응답 샘플 필드: {}", items.get(0).keySet());
        log.info("[SearchService] 버스 응답 샘플 값: {}", items.get(0));
        List<RouteResult> results = new ArrayList<>();
        for (Map<String, Object> item : items) {
            try {
                String depName  = getStringFirstMatch(item, from + "터미널",
                        "depPlaceNm",                           // ← 실제 필드명 (진단 로그 확인)
                        "depTerminalNm", "depTerminalName", "strtpntNm");
                String arrName  = getStringFirstMatch(item, to + "터미널",
                        "arrPlaceNm",                           // ← 실제 필드명 (진단 로그 확인)
                        "arrTerminalNm", "arrTerminalName", "endpntNm");
                String depTime  = formatTime(getStringFirstMatch(item, "",
                        "depPlandTime", "dptrTm", "deptDt", "depHour"));
                String arrTime  = formatTime(getStringFirstMatch(item, "",
                        "arrPlandTime", "arrTm", "arvlDt", "arrHour"));
                String grade    = getStringFirstMatch(item, "고속버스",
                        "gradeNm", "busGradeNm", "busGrdNm", "grade");
                long   fare     = parseLongFirstMatch(item, 0L,
                        "charge", "chargeNm", "adultCharge", "fare", "normalCharge");
                int    duration = calcDurationMinutes(depTime, arrTime);

                results.add(RouteResult.builder()
                        .type("고속버스")
                        .name(grade + " " + depName.replace("터미널", "") + " → " + arrName.replace("터미널", ""))
                        .departureName(depName)
                        .arrivalName(arrName)
                        .departureTime(depTime)
                        .arrivalTime(arrTime)
                        .durationMinutes(duration > 0 ? duration : null)
                        .fare(fare > 0 ? fare : null)
                        .recommendTags(new ArrayList<>())
                        .bookingUrl("https://www.kobus.co.kr")
                        .build());
            } catch (Exception e) {
                log.warn("[SearchService] 버스 데이터 변환 실패: {}", e.getMessage());
            }
        }
        return results;
    }

    /**
     * 카카오 길찾기 API 응답 → RouteResult 변환
     * 응답 구조: { "routes": [{ "summary": { "duration": 초, "distance": 미터, "fare": { "taxi":..., "toll":... } } }] }
     */
    @SuppressWarnings("unchecked")
    private RouteResult convertNaviDataToRouteResult(Map<String, Object> data, String from, String to) {
        if (data == null) return null;
        try {
            List<Map<String, Object>> routes = (List<Map<String, Object>>) data.get("routes");
            if (routes == null || routes.isEmpty()) return null;
            Map<String, Object> summary = (Map<String, Object>) routes.get(0).get("summary");
            if (summary == null) return null;

            int durationSec = parseInt(summary, "duration", 0);
            int durationMin = durationSec / 60;
            double distanceM = parseDouble(summary, "distance", 0.0);
            double distanceKm = Math.round(distanceM / 100.0) / 10.0;

            Map<String, Object> fareMap = (Map<String, Object>) summary.get("fare");
            long toll = fareMap != null ? parseLong(fareMap, "toll", 0L) : 0L;
            // 연비 기준 유류비 추정 (중형차 12km/L, 리터당 1700원)
            long fuelCost = (long) (distanceKm / 12.0 * 1700.0);

            return RouteResult.builder()
                    .type("자가용")
                    .name("자가용")
                    .departureName(from)
                    .arrivalName(to)
                    .durationMinutes(durationMin > 0 ? durationMin : null)
                    .fare(toll + fuelCost)
                    .recommendTags(new ArrayList<>())
                    .bookingUrl(buildKakaoMapUrl(from, to))
                    .detail(RouteResult.CarDetail.builder()
                            .distanceKm(distanceKm)
                            .toll(toll)
                            .fuelCost(fuelCost)
                            .fuelStandard("중형차 연비 12km/L 기준")
                            .build())
                    .build();
        } catch (Exception e) {
            log.warn("[SearchService] 카카오 데이터 변환 실패: {}", e.getMessage());
            return null;
        }
    }

    // ─── Fallback ────────────────────────────────────────────────────────────

    /**
     * 코레일 API 결과에 없는 열차 종류를 실제 시각표(TrainScheduleCache)로 보충.
     * 코레일 API가 완전히 실패하면 foundTypes=빈 Set 으로 호출되어 전체 열차를 시각표로 대체.
     * 시각표에 없는 구간·열차는 표시하지 않는다(임의 출발시각 생성 안 함).
     */
    private List<RouteResult> getSupplementalTrainResults(String from, String to, String time, LocalDate date, Set<String> foundTypes) {
        List<RouteResult> results = new ArrayList<>();
        int fromMin = parseTimeToMinutes(time);
        Map<String, Long> fareCache = new HashMap<>();
        for (String type : List.of("KTX", "ITX-마음", "ITX-새마을", "무궁화호", "SRT")) {
            if (foundTypes.contains(type)) continue;
            // 서울 검색은 TrainScheduleCache 가 용산·청량리 출발도 함께 찾는다
            String dep = type.equals("SRT") ? srtStationName(from) : from;
            String arr = type.equals("SRT") ? srtStationName(to) : to;
            long cityFare = lookupFareFromCsv(type, from, to);
            for (TrainScheduleCache.Trip trip : trainScheduleCache.findTrips(type, dep, arr, date)) {
                if (parseTimeToMinutes(trip.depTime()) < fromMin) continue;
                // 실제 승하차역(용산·광주송정 등) 운임 우선, 없으면 도시 기준 운임
                long fare = fareCache.computeIfAbsent(type + trip.depStation() + "|" + trip.arrStation(),
                        k -> lookupFareFromCsv(type, trip.depStation(), trip.arrStation()));
                if (fare <= 0) fare = cityFare;
                results.add(RouteResult.builder()
                        .type(type).name(type + " " + trip.trainNo())
                        .departureName(normalizeCity(trip.depStation()) + "역")
                        .arrivalName(normalizeCity(trip.arrStation()) + "역")
                        .departureTime(trip.depTime()).arrivalTime(trip.arrTime())
                        .durationMinutes(trip.minutes()).fare(fare > 0 ? fare : null)
                        .recommendTags(new ArrayList<>())
                        .bookingUrl(type.equals("SRT") ? "https://etk.srail.kr" : "https://www.letskorail.com")
                        .build());
            }
        }
        return results;
    }

    /** 열차 API 실패 시: 시각표(엑셀) → 시각표 파일도 없으면 mock. 시각표는 있는데 결과가 없으면 열차 미운행 구간. */
    private List<RouteResult> trainFallbackResults(String from, String to, String date, String time, LocalDate searchDate) {
        if (trainScheduleCache.hasTimetable()) return getSupplementalTrainResults(from, to, time, searchDate, Set.of());
        return mockTrainResults(from, to, date, time);
    }

    private List<RouteResult> mockTrainResults(String from, String to, String date, String time) {
        log.warn("[SearchService] 열차 시각표 없음 → mock 열차 데이터");
        return mockDataProvider.getMockSearchResult(from, to, date, time).getResults().stream()
                .filter(r -> !"고속버스".equals(r.getType()) && !"자가용".equals(r.getType()))
                .collect(Collectors.toList());
    }

    private String normalizeCity(String name) {
        if (name == null) return "";
        return name.replace("역", "").replace("터미널", "").trim();
    }

    /** 호남선·전라선 방면 도시 여부 (ITX-새마을·무궁화호는 용산 출발) */
    private boolean isHonamLineCity(String city) {
        return Set.of("목포", "광주", "나주", "정읍", "전주", "익산", "논산",
                      "여수", "순천").contains(city);
    }

    /**
     * 출발지/도착지 조합에 따라 추가로 조회해야 할 출발역 코드 반환.
     * null = 추가 조회 불필요.
     *
     * 호남선: ITX-새마을·무궁화호는 서울역(3900023)이 아닌 용산역(3900024) 출발.
     * 서울 → 광주/목포/전주/익산 등 조회 시 용산 코드로 한 번 더 조회해야 해당 열차가 잡힘.
     */
    private String getAltDepCode(String from, String to) {
        String f = normalizeCity(from);
        String t = normalizeCity(to);
        if (f.equals("서울") && isHonamLineCity(t)) return "3900024"; // 용산역
        return null;
    }

    /**
     * 역방향(호남선→서울)에서 도착지 코드 보완.
     * ITX-새마을·무궁화호는 용산역 도착 → arrCode=서울(3900023)로 조회하면 누락.
     */
    private String getAltArrCode(String from, String to) {
        String f = normalizeCity(from);
        String t = normalizeCity(to);
        if (isHonamLineCity(f) && (t.equals("서울") || t.equals("수서"))) return "3900024"; // 용산역
        return null;
    }

    /** SRT 역명: 서울권은 수서 */
    private String srtStationName(String city) {
        String c = normalizeCity(city);
        return c.equals("서울") ? "수서" : c;
    }

    /**
     * 열차 종류별 운임 조회.
     * 무궁화호·ITX-새마을은 실제 운임표에서 직접 조회.
     * 경로 매칭 실패 시 KTX 운임 비율로 fallback.
     */
    private long lookupFareFromCsv(String trainType, String from, String to) {
        if (trainType == null) return 0L;

        // KTX 운임 (기준값)
        long ktxFare = fallbackFareLoader.loadKtxFares().stream()
                .filter(f -> stationMatches(f.getDepartureName(), from) && stationMatches(f.getArrivalName(), to))
                .findFirst().map(FareDto::getFare).orElse(0L);

        return switch (trainType) {
            case "KTX", "KTX-산천", "KTX-이음" -> ktxFare;
            case "SRT" -> {
                long f = fallbackFareLoader.loadSrtFares().stream()
                        .filter(d -> srtStationMatches(d.getDepartureName(), from) && stationMatches(d.getArrivalName(), to))
                        .findFirst().map(FareDto::getFare).orElse(0L);
                yield f > 0 ? f : (ktxFare > 0 ? Math.round(ktxFare * 0.88) : 0L);
            }
            case "ITX-새마을", "ITX-청춘" -> {
                long f = fallbackFareLoader.loadItxFares().stream()
                        .filter(d -> stationMatches(d.getDepartureName(), from) && stationMatches(d.getArrivalName(), to))
                        .findFirst().map(FareDto::getFare).orElse(0L);
                yield f > 0 ? f : (ktxFare > 0 ? Math.round(ktxFare * 0.73) : 0L);
            }
            case "ITX-마음" -> {
                // ITX-마음은 별도 운임표 없음 → ITX-새마을 대비 약 89% 수준
                long itx = fallbackFareLoader.loadItxFares().stream()
                        .filter(d -> stationMatches(d.getDepartureName(), from) && stationMatches(d.getArrivalName(), to))
                        .findFirst().map(FareDto::getFare).orElse(0L);
                yield itx > 0 ? Math.round(itx * 0.89) : (ktxFare > 0 ? Math.round(ktxFare * 0.65) : 0L);
            }
            case "무궁화호", "누리로" -> {
                long f = fallbackFareLoader.loadMugunghwaFares().stream()
                        .filter(d -> stationMatches(d.getDepartureName(), from) && stationMatches(d.getArrivalName(), to))
                        .findFirst().map(FareDto::getFare).orElse(0L);
                yield f > 0 ? f : (ktxFare > 0 ? Math.round(ktxFare * 0.56) : 0L);
            }
            default -> 0L;
        };
    }

    /** 역명 유연 매칭 (역 접미사 제거, 포함 관계 허용) */
    private boolean stationMatches(String stationName, String cityName) {
        if (stationName == null || cityName == null) return false;
        String s = stationName.replace("역", "").replace("터미널", "").trim();
        String c = cityName.replace("역", "").replace("터미널", "").trim();
        // 광주/광주송정 혼동 방지: 완전 일치 우선, 한 쪽이 다른 쪽의 접두어인 경우 엄격 비교
        if (s.equals(c)) return true;
        // "광주" vs "광주송정" 오매칭 차단: 특정 역명 쌍은 포함 관계로 매칭하지 않음
        if ((s.equals("광주") && c.equals("광주송정")) || (s.equals("광주송정") && c.equals("광주"))) return false;
        return s.contains(c) || c.contains(s);
    }

    /** SRT 전용 매칭: 수서↔서울을 동일역으로 취급 */
    private boolean srtStationMatches(String stationName, String cityName) {
        if (stationMatches(stationName, cityName)) return true;
        String s = stationName != null ? stationName.replace("역", "").trim() : "";
        String c = cityName    != null ? cityName.replace("역", "").trim()    : "";
        return (s.equals("수서") && c.equals("서울"))
            || (s.equals("서울") && c.equals("수서"));
    }

    /** 버스 시간표 데이터가 없으므로 출발시각 없이 구간 기준값 1건만 반환(자가용과 동일). */
    private List<RouteResult> getBusFallbackResults(String from, String to, String time) {
        String f = normalizeCity(from);
        String t = normalizeCity(to);
        long[] row = BUS_FALLBACK.get(f + "|" + t);
        if (row == null) row = BUS_FALLBACK.get(t + "|" + f);
        int dur  = row != null ? (int) row[0] : 270;
        long fare = row != null ? row[1] : 25900L;

        List<RouteResult> results = new ArrayList<>();
        results.add(RouteResult.builder()
            .type("고속버스").name("우등")
            .departureName(from + "터미널").arrivalName(to + "터미널")
            .durationMinutes(dur).fare(fare)
            .recommendTags(new ArrayList<>())
            .bookingUrl("https://www.kobus.co.kr")
            .build());
        return results;
    }

    private RouteResult getCarFallbackResult(String from, String to) {
        String f = normalizeCity(from);
        String t = normalizeCity(to);
        long[] row = CAR_FALLBACK.get(f + "|" + t);
        if (row == null) row = CAR_FALLBACK.get(t + "|" + f);

        int dur       = row != null ? (int) row[0] : 180;
        double distKm = row != null ? row[1] / 10.0 : 200.0;
        long toll     = row != null ? row[2] : 10000L;
        long fuel     = row != null ? row[3] : 28000L;

        return RouteResult.builder()
                .type("자가용").name("자가용")
                .departureName(from).arrivalName(to)
                .durationMinutes(dur).fare(toll + fuel)
                .recommendTags(new ArrayList<>())
                .bookingUrl(buildKakaoMapUrl(from, to))
                .detail(RouteResult.CarDetail.builder()
                        .distanceKm(distKm)
                        .toll(toll)
                        .fuelCost(fuel)
                        .fuelStandard("중형차 연비 12km/L 기준")
                        .build())
                .build();
    }

    // ─── Summary 빌드 ────────────────────────────────────────────────────────

    private RouteSearchResponse.Summary buildSummary(List<RouteResult> results) {
        RouteResult cheapest = results.stream()
                .filter(r -> r.getFare() != null && r.getFare() > 0)
                .min(Comparator.comparingLong(RouteResult::getFare))
                .orElse(null);
        RouteResult fastest = results.stream()
                .filter(r -> r.getDurationMinutes() != null && r.getDurationMinutes() > 0)
                .min(Comparator.comparingInt(RouteResult::getDurationMinutes))
                .orElse(null);

        // 종합추천: 가격·시간 정규화 점수 최소값 (각 50% 가중치)
        long maxFare = results.stream().filter(r -> r.getFare() != null && r.getFare() > 0)
                .mapToLong(RouteResult::getFare).max().orElse(1L);
        int maxDur  = results.stream().filter(r -> r.getDurationMinutes() != null && r.getDurationMinutes() > 0)
                .mapToInt(RouteResult::getDurationMinutes).max().orElse(1);
        final long mf = maxFare;
        final int  md = maxDur;
        RouteResult recommended = results.stream()
                .filter(r -> r.getFare() != null && r.getFare() > 0
                          && r.getDurationMinutes() != null && r.getDurationMinutes() > 0)
                .min(Comparator.comparingDouble(r ->
                        0.5 * r.getFare() / mf + 0.5 * r.getDurationMinutes() / md))
                .orElse(fastest);

        String cheapestType     = cheapest     != null ? cheapest.getType()     : null;
        String fastestType      = fastest      != null ? fastest.getType()      : null;
        String recommendedType  = recommended  != null ? recommended.getType()  : null;

        results.forEach(r -> r.getRecommendTags().clear());
        if (cheapest    != null) cheapest.getRecommendTags().add("최저가");
        if (fastest     != null) fastest.getRecommendTags().add("최단시간");
        if (recommended != null && !recommended.getRecommendTags().contains("종합추천")) {
            recommended.getRecommendTags().add("종합추천");
        }

        return RouteSearchResponse.Summary.builder()
                .cheapest(cheapestType).fastest(fastestType).recommended(recommendedType)
                .build();
    }

    // ─── 유틸 ─────────────────────────────────────────────────────────────────

    private String getString(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    /** 여러 키를 순서대로 시도하여 처음 발견된 non-null, non-empty 값 반환 */
    private String getStringFirstMatch(Map<String, Object> map, String defaultVal, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null && !val.toString().isBlank()) return val.toString();
        }
        return defaultVal;
    }

    /** 여러 키를 순서대로 시도하여 처음 발견된 양수 값 반환 */
    private long parseLongFirstMatch(Map<String, Object> map, long defaultVal, String... keys) {
        for (String key : keys) {
            try {
                Object val = map.get(key);
                if (val == null) continue;
                long v = Long.parseLong(val.toString().replaceAll("[^0-9]", ""));
                if (v > 0) return v;
            } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    private long parseLong(Map<String, Object> map, String key, long defaultVal) {
        try {
            Object val = map.get(key);
            if (val == null) return defaultVal;
            return Long.parseLong(val.toString().replaceAll("[^0-9]", ""));
        } catch (Exception e) { return defaultVal; }
    }

    private int parseInt(Map<String, Object> map, String key, int defaultVal) {
        try {
            Object val = map.get(key);
            if (val == null) return defaultVal;
            return (int) Double.parseDouble(val.toString());
        } catch (Exception e) { return defaultVal; }
    }

    private double parseDouble(Map<String, Object> map, String key, double defaultVal) {
        try {
            Object val = map.get(key);
            if (val == null) return defaultVal;
            return Double.parseDouble(val.toString());
        } catch (Exception e) { return defaultVal; }
    }

    /**
     * 다양한 시각 포맷 → "HH:mm" 변환.
     * - "HH:mm" / "HH:mm:ss"  → 그대로
     * - "yyyy-MM-dd HH:mm:ss" → 시각 부분 추출
     * - "YYYYMMDDHHmmss" (14자리) / "YYYYMMDDHHmm" (12자리) → 8번째 이후 파싱
     * - "HHmmss" (6자리) / "HHmm" (4자리) → 직접 파싱
     */
    private String formatTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        raw = raw.trim();
        // "HH:mm" or "HH:mm:ss"
        if (raw.matches("\\d{2}:\\d{2}.*")) return raw.substring(0, 5);
        // "yyyy-MM-dd HH:mm:ss" or "yyyy-MM-dd HH:mm:ss.S"
        if (raw.contains(" ") && raw.length() >= 16) return raw.substring(11, 16);
        // "yyyy-MM-ddTHH:mm:ss"
        if (raw.contains("T") && raw.length() >= 16) return raw.substring(11, 16);
        // 순수 숫자만 남겨서 처리
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() >= 12) digits = digits.substring(8); // YYYYMMDDHHmm... → HHmm...
        if (digits.length() >= 4) return digits.substring(0, 2) + ":" + digits.substring(2, 4);
        return null;
    }

    /**
     * 두 시각 문자열("HH:mm")로 소요 시간(분) 계산
     */
    private int calcDurationMinutes(String dep, String arr) {
        try {
            if (dep == null || arr == null) return 0;
            String[] d = dep.split(":");
            String[] a = arr.split(":");
            int depMin = Integer.parseInt(d[0]) * 60 + Integer.parseInt(d[1]);
            int arrMin = Integer.parseInt(a[0]) * 60 + Integer.parseInt(a[1]);
            int diff = arrMin - depMin;
            return diff > 0 ? diff : diff + 24 * 60; // 자정 넘김 처리
        } catch (Exception e) { return 0; }
    }

    private boolean isKtxType(String grade) {
        return grade != null && (grade.contains("KTX") || grade.contains("SRT"));
    }

    /**
     * 코레일 열차번호 체계로 종류 추론 (travelerTrainRunPlan API는 열차종류명 미제공)
     * - KTX: 1-99, 101-199, 401-499, 4501-4599, 5001-5099
     * - ITX-새마을: 1001-1099
     * - ITX-마음: 1101-1149 (경강선 전용)
     * - 무궁화호: 1150-1999
     * - 누리로: 2001-2199
     */
    private String inferTrainTypeFromNo(String trainNoRaw) {
        if (trainNoRaw == null || trainNoRaw.isBlank()) return "열차";
        try {
            int no = Integer.parseInt(trainNoRaw.replaceAll("[^0-9]", ""));
            if ((no >= 1   && no <= 99)
             || (no >= 101 && no <= 199)
             || (no >= 401 && no <= 499)
             || (no >= 4501 && no <= 4599)
             || (no >= 5001 && no <= 5099)) return "KTX";
            if (no >= 4001 && no <= 4099) return "ITX-청춘";
        if (no >= 1001 && no <= 1099) return "ITX-새마을";
            if (no >= 1101 && no <= 1149) return "ITX-마음";
            if (no >= 1150 && no <= 1999) return "무궁화호";
            if (no >= 2001 && no <= 2199) return "누리로";
        } catch (Exception ignored) {}
        return "열차";
    }

    /**
     * 카카오맵 길찾기 URL 생성
     * LocationCodeMapper의 좌표는 "경도,위도" 형식 → 카카오맵 URL은 "위도,경도" 순서
     */
    private String buildKakaoMapUrl(String from, String to) {
        String fromCoords = locationCodeMapper.toCoordinates(from);
        String toCoords   = locationCodeMapper.toCoordinates(to);
        if (fromCoords != null && toCoords != null) {
            return "https://map.kakao.com/link/from/" + from + "," + swapLngLat(fromCoords)
                 + "/to/" + to + "," + swapLngLat(toCoords);
        }
        return "https://map.kakao.com";
    }

    private String swapLngLat(String lngLat) {
        String[] p = lngLat.split(",");
        return p[1].trim() + "," + p[0].trim();
    }

    private static LocalDate toLocalDate(String date) {
        try { return LocalDate.parse(date); } catch (Exception e) { return LocalDate.now(); }
    }

    private int parseTimeToMinutes(String time) {
        return TimeUtil.parseTimeToMinutes(time);
    }
}
