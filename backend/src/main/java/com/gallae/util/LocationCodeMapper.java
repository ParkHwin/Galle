package com.gallae.util;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class LocationCodeMapper {

    // 도시명 → 코레일 역 코드 (travelerTrainRunPlan2 cond[dptre_stn_cd::EQ])
    // 7자리 역코드: /codes2?cond[type::EQ]=stn_cd 로 조회 가능
    private static final Map<String, String> KORAIL_CODES = Map.ofEntries(
        Map.entry("서울",    "3900023"),
        Map.entry("용산",    "3900024"),
        Map.entry("영등포",  "3900025"),
        Map.entry("광명",    "3900026"),
        Map.entry("수원",    "3900029"),
        Map.entry("평택",    "3900032"),
        Map.entry("천안아산","3900036"),
        Map.entry("오송",    "3900040"),
        Map.entry("대전",    "3900045"),
        Map.entry("서대전",  "3900046"),
        Map.entry("김천구미","3900067"),
        Map.entry("동대구",  "3900077"),
        Map.entry("대구",    "3900078"),
        Map.entry("경주",    "3900082"),
        Map.entry("울산",    "3900083"),
        Map.entry("부산",    "3900114"),
        Map.entry("광주송정","3900089"),
        Map.entry("광주",    "3900088"),
        Map.entry("익산",    "3900080"),
        Map.entry("전주",    "3900081"),
        Map.entry("목포",    "3900094"),
        Map.entry("여수",    "3900097"),
        Map.entry("순천",    "3900096"),
        Map.entry("나주",    "3900091"),
        Map.entry("강릉",    "3900007"),
        Map.entry("춘천",    "3900009"),
        Map.entry("원주",    "3900013"),
        Map.entry("행신",    "3900022"),
        Map.entry("포항",    "3900085"),
        Map.entry("진주",    "3900112"),
        Map.entry("안동",    "3900062")
    );

    // 도시명 → 고속버스 터미널 ID (GetStrtpntAlocFndExpbusInfo depTerminalId/arrTerminalId)
    // 실제 ID는 /GetExpBusTrminlList?terminalNm=xxx 로 조회 가능
    private static final Map<String, String> BUS_TERMINAL_IDS = Map.ofEntries(
        Map.entry("서울",  "NAEK010"),  // 서울경부
        Map.entry("부산",  "NAEK300"),  // 부산
        Map.entry("대전",  "NAEK140"),  // 대전
        Map.entry("대구",  "NAEK200"),  // 대구
        Map.entry("광주",  "NAEK260"),  // 광주
        Map.entry("인천",  "NAEK030"),  // 인천
        Map.entry("수원",  "NAEK040"),  // 수원
        Map.entry("울산",  "NAEK290"),  // 울산
        Map.entry("전주",  "NAEK240"),  // 전주
        Map.entry("청주",  "NAEK150"),  // 청주
        Map.entry("창원",  "NAEK310"),  // 창원
        Map.entry("춘천",  "NAEK080"),  // 춘천
        Map.entry("강릉",  "NAEK090"),  // 강릉
        Map.entry("목포",  "NAEK250"),  // 목포
        Map.entry("여수",  "NAEK270"),  // 여수
        Map.entry("천안",  "NAEK130"),  // 천안
        Map.entry("포항",  "NAEK320"),  // 포항
        Map.entry("진주",  "NAEK330"),  // 진주
        Map.entry("안동",  "NAEK210"),  // 안동
        Map.entry("제주",  "NAEK380")   // 제주
    );

    // 도시명 → 카카오 Navi API 좌표 (경도,위도)
    private static final Map<String, String> COORDINATES = Map.ofEntries(
        Map.entry("서울", "126.9780,37.5665"),
        Map.entry("광명", "126.8658,37.4784"),
        Map.entry("부산", "129.0756,35.1796"),
        Map.entry("대전", "127.3845,36.3504"),
        Map.entry("대구", "128.6014,35.8714"),
        Map.entry("광주", "126.8516,35.1595"),
        Map.entry("제주", "126.5312,33.4996"),
        Map.entry("인천", "126.7052,37.4563"),
        Map.entry("수원", "127.0286,37.2636"),
        Map.entry("울산", "129.3114,35.5384"),
        Map.entry("전주", "127.1530,35.8242"),
        Map.entry("청주", "127.4912,36.6424"),
        Map.entry("창원", "128.6811,35.2280"),
        Map.entry("춘천", "127.7298,37.8813"),
        Map.entry("강릉", "128.8761,37.7519"),
        Map.entry("목포", "126.3922,34.8118"),
        Map.entry("여수", "127.6622,34.7604"),
        Map.entry("순천", "127.4875,34.9507"),
        Map.entry("나주", "126.7101,35.0160"),
        Map.entry("천안", "127.1548,36.8151"),
        Map.entry("익산", "126.9547,35.9483"),
        Map.entry("원주", "127.9199,37.3422"),
        Map.entry("경주", "129.2114,35.8562"),
        Map.entry("포항", "129.3682,36.0194"),
        Map.entry("안동", "128.7256,36.5684"),
        Map.entry("진주", "128.1076,35.1804"),
        Map.entry("평택", "127.0677,36.9947"),
        Map.entry("수서", "127.1030,37.4844")
    );

    /** 도시명 → 코레일 역 코드. 이미 숫자 코드면 그대로 반환. 매핑 없으면 null. */
    public String toKorailStationCode(String cityName) {
        if (cityName == null) return null;
        if (cityName.matches("\\d{4,7}")) return cityName; // 4~7자리 숫자 코드 직접 허용
        return KORAIL_CODES.get(cityName);
    }

    /** 도시명 → 고속버스 터미널 ID. 이미 숫자면 그대로 반환. 매핑 없으면 null. */
    public String toBusTerminalId(String cityName) {
        if (cityName == null) return null;
        if (cityName.matches("\\d+")) return cityName;
        return BUS_TERMINAL_IDS.get(cityName);
    }

    /** 도시명 → "경도,위도" 좌표 문자열. 이미 좌표 형식이면 그대로 반환. 매핑 없으면 null. */
    public String toCoordinates(String cityName) {
        if (cityName == null) return null;
        if (cityName.matches("[0-9.]+,[0-9.]+")) return cityName;
        return COORDINATES.get(cityName);
    }
}
