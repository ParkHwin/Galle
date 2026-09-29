package com.gallae.util;

import com.gallae.dto.RouteResult;
import com.gallae.dto.RouteSearchResponse;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MockDataProvider {

    public RouteSearchResponse getMockSearchResult(String from, String to, String date, String time) {
        int base = TimeUtil.parseTimeToMinutes(time);
        List<RouteResult> results = new ArrayList<>();

        // KTX — 3편
        int[] ktxOffsets = {20, 65, 115};
        for (int offset : ktxOffsets) {
            int dep = (base + offset) % 1440;
            results.add(RouteResult.builder()
                    .type("KTX").name("KTX")
                    .departureName(from + "역").arrivalName(to + "역")
                    .departureTime(TimeUtil.minutesToTime(dep)).arrivalTime(TimeUtil.minutesToTime((dep + 138) % 1440))
                    .durationMinutes(138).fare(59800L)
                    .recommendTags(new ArrayList<>())
                    .bookingUrl("https://www.letskorail.com")
                    .build());
        }

        // SRT — 2편
        int[] srtOffsets = {40, 100};
        for (int offset : srtOffsets) {
            int dep = (base + offset) % 1440;
            results.add(RouteResult.builder()
                    .type("SRT").name("SRT")
                    .departureName("수서역").arrivalName(to + "역")
                    .departureTime(TimeUtil.minutesToTime(dep)).arrivalTime(TimeUtil.minutesToTime((dep + 135) % 1440))
                    .durationMinutes(135).fare(52600L)
                    .recommendTags(new ArrayList<>())
                    .bookingUrl("https://etk.srail.kr")
                    .build());
        }

        // ITX-새마을 — 1편
        int itxDep = (base + 30) % 1440;
        results.add(RouteResult.builder()
                .type("ITX-새마을").name("ITX-새마을")
                .departureName(from + "역").arrivalName(to + "역")
                .departureTime(TimeUtil.minutesToTime(itxDep)).arrivalTime(TimeUtil.minutesToTime((itxDep + 282) % 1440))
                .durationMinutes(282).fare(42600L)
                .recommendTags(new ArrayList<>())
                .bookingUrl("https://www.letskorail.com")
                .build());

        // 고속버스 — 3편
        int[] busOffsets = {30, 120, 210};
        for (int offset : busOffsets) {
            int dep = (base + offset) % 1440;
            results.add(RouteResult.builder()
                    .type("고속버스").name(from + " → " + to)
                    .departureName(from + "터미널").arrivalName(to + "터미널")
                    .departureTime(TimeUtil.minutesToTime(dep)).arrivalTime(TimeUtil.minutesToTime((dep + 270) % 1440))
                    .durationMinutes(270).fare(25900L)
                    .recommendTags(new ArrayList<>())
                    .bookingUrl("https://www.kobus.co.kr")
                    .build());
        }

        // 자가용 — 시간 무관
        results.add(RouteResult.builder()
                .type("자가용").name("자가용 (경부고속도로)")
                .departureName(from).arrivalName(to)
                .durationMinutes(280).fare(58100L)
                .recommendTags(new ArrayList<>())
                .bookingUrl("https://map.kakao.com/?q=" + to)
                .detail(RouteResult.CarDetail.builder()
                        .distanceKm(325.0).toll(28100L).fuelCost(30000L)
                        .fuelStandard("중형차 연비 12km/L 기준")
                        .build())
                .build());

        return RouteSearchResponse.builder()
                .from(from).to(to).date(date).time(time)
                .results(results)
                .summary(RouteSearchResponse.Summary.builder()
                        .cheapest("고속버스").fastest("KTX").recommended("KTX")
                        .build())
                .disclaimer("표시된 가격은 참고용이며 실제 가격은 각 예매 사이트에서 확인하세요.")
                .build();
    }
}
