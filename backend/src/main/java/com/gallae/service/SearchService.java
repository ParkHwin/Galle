package com.gallae.service;

import com.gallae.client.*;
import com.gallae.dto.*;
import com.gallae.exception.ExternalApiException;
import com.gallae.util.MockDataProvider;
import com.gallae.util.FallbackFareLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final KorailClient korailClient;
    private final BusClient busClient;
    private final KakaoNaviClient kakaoNaviClient;
    private final MockDataProvider mockDataProvider;
    private final FallbackFareLoader fallbackFareLoader;

    public RouteSearchResponse search(String from, String to, String date, String time) {
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            throw new IllegalArgumentException("출발지와 도착지를 입력해주세요.");
        }

        log.info("[SearchService] 검색 시작: {} -> {} / {} {}", from, to, date, time);

        // 외부 API가 없을 때 mock 데이터 반환
        // 실제 API 키 설정 후 아래 로직 활성화
        try {
            return buildSearchResponse(from, to, date, time);
        } catch (Exception e) {
            log.warn("[SearchService] 실제 API 실패, mock 데이터 반환: {}", e.getMessage());
            return mockDataProvider.getMockSearchResult(from, to, date, time);
        }
    }

    private RouteSearchResponse buildSearchResponse(String from, String to, String date, String time) {
        List<RouteResult> results = new ArrayList<>();

        // 1. 열차 스케줄 (코레일 API)
        CompletableFuture<List<RouteResult>> trainFuture = CompletableFuture.supplyAsync(() -> {
            try {
                korailClient.fetchTrainRunPlan(date, from, to);
                return Collections.emptyList();
            } catch (ExternalApiException e) {
                log.warn("[SearchService] 코레일 API 실패, fallback 사용: {}", e.getMessage());
                return getTrainFallbackResults(from, to);
            }
        });

        // 2. 고속버스 (TAGO API)
        CompletableFuture<List<RouteResult>> busFuture = CompletableFuture.supplyAsync(() -> {
            try {
                busClient.fetchBusRoutes(from, to, date);
                return Collections.emptyList();
            } catch (ExternalApiException e) {
                log.warn("[SearchService] 버스 API 실패, fallback 사용: {}", e.getMessage());
                return getBusFallbackResults(from, to);
            }
        });

        // 3. 자가용 (카카오 API)
        CompletableFuture<RouteResult> carFuture = CompletableFuture.supplyAsync(() -> {
            try {
                kakaoNaviClient.fetchDirections(from, to);
                return null;
            } catch (ExternalApiException e) {
                log.warn("[SearchService] 카카오 API 실패, fallback 사용: {}", e.getMessage());
                return getCarFallbackResult(from, to);
            }
        });

        // 결과 합치기
        try {
            results.addAll(trainFuture.get());
            results.addAll(busFuture.get());
            RouteResult carResult = carFuture.get();
            if (carResult != null) results.add(carResult);
        } catch (Exception e) {
            log.error("[SearchService] 결과 취합 실패: {}", e.getMessage());
            return mockDataProvider.getMockSearchResult(from, to, date, time);
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

    private List<RouteResult> getTrainFallbackResults(String from, String to) {
        List<FareDto> ktxFares = fallbackFareLoader.loadKtxFares();
        List<FareDto> srtFares = fallbackFareLoader.loadSrtFares();
        List<RouteResult> results = new ArrayList<>();

        ktxFares.stream()
            .filter(f -> f.getDepartureName().contains(from) && f.getArrivalName().contains(to))
            .findFirst()
            .ifPresent(f -> results.add(RouteResult.builder()
                .type("KTX").name("KTX")
                .departureName(f.getDepartureName()).arrivalName(f.getArrivalName())
                .durationMinutes(138).fare(f.getFare())
                .recommendTags(new ArrayList<>())
                .bookingUrl("https://www.letskorail.com")
                .build()));

        srtFares.stream()
            .filter(f -> f.getDepartureName().contains(from) && f.getArrivalName().contains(to))
            .findFirst()
            .ifPresent(f -> results.add(RouteResult.builder()
                .type("SRT").name("SRT")
                .departureName(f.getDepartureName()).arrivalName(f.getArrivalName())
                .durationMinutes(135).fare(f.getFare())
                .recommendTags(new ArrayList<>())
                .bookingUrl("https://etk.srail.kr")
                .build()));

        return results;
    }

    private List<RouteResult> getBusFallbackResults(String from, String to) {
        return List.of(RouteResult.builder()
                .type("고속버스").name(from + " → " + to)
                .departureName(from + "터미널").arrivalName(to + "터미널")
                .departureTime("09:00").arrivalTime("13:30")
                .durationMinutes(270).fare(25900L)
                .recommendTags(List.of("최저가"))
                .bookingUrl("https://www.kobus.co.kr")
                .build());
    }

    private RouteResult getCarFallbackResult(String from, String to) {
        return RouteResult.builder()
                .type("자가용").name("자가용")
                .departureName(from).arrivalName(to)
                .durationMinutes(280).fare(58100L)
                .recommendTags(new ArrayList<>())
                .detail(RouteResult.CarDetail.builder()
                        .distanceKm(325.0).toll(28100L).fuelCost(30000L)
                        .fuelStandard("중형차 연비 12km/L 기준")
                        .build())
                .build();
    }

    private RouteSearchResponse.Summary buildSummary(List<RouteResult> results) {
        RouteResult cheapest = results.stream()
                .min(Comparator.comparingLong(r -> r.getFare() != null ? r.getFare() : Long.MAX_VALUE))
                .orElse(null);
        RouteResult fastest = results.stream()
                .filter(r -> r.getDurationMinutes() != null)
                .min(Comparator.comparingInt(RouteResult::getDurationMinutes))
                .orElse(null);

        String cheapestType = cheapest != null ? cheapest.getType() : null;
        String fastestType = fastest != null ? fastest.getType() : null;

        // 종합 추천: 빠른 것 우선
        String recommended = fastestType;

        // 추천 태그 설정
        results.forEach(r -> r.getRecommendTags().clear());
        if (cheapest != null) cheapest.getRecommendTags().add("최저가");
        if (fastest != null && !fastest.equals(cheapest)) fastest.getRecommendTags().add("최단시간");
        if (fastest != null) fastest.getRecommendTags().add("종합추천");

        return RouteSearchResponse.Summary.builder()
                .cheapest(cheapestType).fastest(fastestType).recommended(recommended)
                .build();
    }
}
