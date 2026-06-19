package com.gallae.util;

import com.gallae.dto.RouteResult;
import com.gallae.dto.RouteSearchResponse;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MockDataProvider {

    public RouteSearchResponse getMockSearchResult(String from, String to, String date, String time) {
        List<RouteResult> results = new ArrayList<>();

        // KTX
        results.add(RouteResult.builder()
                .type("KTX")
                .name("KTX 101")
                .departureName(from + "역")
                .arrivalName(to + "역")
                .departureTime("09:00")
                .arrivalTime("11:18")
                .durationMinutes(138)
                .fare(59800L)
                .recommendTags(List.of("최단시간", "종합추천"))
                .bookingUrl("https://www.letskorail.com")
                .build());

        // SRT
        results.add(RouteResult.builder()
                .type("SRT")
                .name("SRT 301")
                .departureName("수서역")
                .arrivalName(to + "역")
                .departureTime("09:10")
                .arrivalTime("11:25")
                .durationMinutes(135)
                .fare(52600L)
                .recommendTags(new ArrayList<>())
                .bookingUrl("https://etk.srail.kr")
                .build());

        // ITX-새마을
        results.add(RouteResult.builder()
                .type("ITX-새마을")
                .name("ITX-새마을 1001")
                .departureName(from + "역")
                .arrivalName(to + "역")
                .departureTime("08:40")
                .arrivalTime("13:22")
                .durationMinutes(282)
                .fare(42600L)
                .recommendTags(new ArrayList<>())
                .bookingUrl("https://www.letskorail.com")
                .build());

        // 고속버스
        results.add(RouteResult.builder()
                .type("고속버스")
                .name(from + " → " + to)
                .departureName(from + "터미널")
                .arrivalName(to + "터미널")
                .departureTime("09:20")
                .arrivalTime("13:40")
                .durationMinutes(260)
                .fare(25900L)
                .recommendTags(List.of("최저가"))
                .bookingUrl("https://www.kobus.co.kr")
                .build());

        // 자가용
        results.add(RouteResult.builder()
                .type("자가용")
                .name("자가용 (경부고속도로)")
                .departureName(from)
                .arrivalName(to)
                .departureTime(null)
                .arrivalTime(null)
                .durationMinutes(280)
                .fare(58100L)
                .recommendTags(new ArrayList<>())
                .bookingUrl(null)
                .detail(RouteResult.CarDetail.builder()
                        .distanceKm(325.0)
                        .toll(28100L)
                        .fuelCost(30000L)
                        .fuelStandard("중형차 연비 12km/L 기준")
                        .build())
                .build());

        return RouteSearchResponse.builder()
                .from(from)
                .to(to)
                .date(date)
                .time(time)
                .results(results)
                .summary(RouteSearchResponse.Summary.builder()
                        .cheapest("고속버스")
                        .fastest("KTX")
                        .recommended("KTX")
                        .build())
                .disclaimer("표시된 가격은 참고용이며 실제 가격은 각 예매 사이트에서 확인하세요.")
                .build();
    }
}
