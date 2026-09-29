package com.gallae;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * 실제 API 응답의 필드명을 확인하는 진단용 테스트.
 * 인터넷 연결이 필요합니다.
 * VSCode 터미널에서: ./gradlew test --tests "com.gallae.ApiFieldDiagnosticTest"
 *
 * ⚠️ 주의: SERVICE_KEY는 환경변수 KORAIL_SERVICE_KEY 또는 application-local.yml에서 읽습니다.
 *   실제 키를 이 파일에 직접 하드코딩하지 마세요.
 */
public class ApiFieldDiagnosticTest {

    /** 환경변수 KORAIL_SERVICE_KEY → 없으면 빈 문자열 (테스트 스킵됨) */
    private static final String SERVICE_KEY =
            System.getenv().getOrDefault("KORAIL_SERVICE_KEY", "");

    @Test
    @SuppressWarnings("unchecked")
    void 코레일_응답필드_확인() throws Exception {
        String today = java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        String url = "https://apis.data.go.kr/B551457/run/v2/travelerTrainRunPlan2"
                + "?serviceKey=" + SERVICE_KEY
                + "&pageNo=1&numOfRows=3&returnType=JSON"
                + "&cond%5Brun_ymd%3A%3AGTE%5D=" + today
                + "&cond%5Brun_ymd%3A%3ALTE%5D=" + today
                + "&cond%5Bdptre_stn_cd%3A%3AEQ%5D=3900023"  // 서울
                + "&cond%5Barvl_stn_cd%3A%3AEQ%5D=3900114"; // 부산

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("=== HTTP 상태코드: " + response.statusCode() + " ===");
        System.out.println("=== 코레일 raw 응답 ===");
        System.out.println(response.body());

        if (response.statusCode() == 200) {
            ObjectMapper om = new ObjectMapper();
            Map<String, Object> json = om.readValue(response.body(), Map.class);
            System.out.println("\n=== 최상위 키 목록 ===");
            System.out.println(json.keySet());

            Object data = json.get("data");
            if (data instanceof List && !((List<?>) data).isEmpty()) {
                Map<String, Object> first = (Map<String, Object>) ((List<?>) data).get(0);
                System.out.println("\n=== 첫 번째 아이템 키 목록 ===");
                System.out.println(first.keySet());
                System.out.println("\n=== 첫 번째 아이템 값 ===");
                first.forEach((k, v) -> System.out.printf("  %-30s = %s%n", k, v));
            } else {
                System.out.println("data 배열 없음. 'response' 키 시도:");
                Object resp = json.get("response");
                if (resp instanceof Map) {
                    System.out.println(resp);
                }
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void 버스_응답필드_확인() throws Exception {
        String today = java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        String url = "https://apis.data.go.kr/1613000/ExpBusInfo/GetStrtpntAlocFndExpbusInfo"
                + "?serviceKey=" + SERVICE_KEY
                + "&depTerminalId=NAEK010"  // 서울
                + "&arrTerminalId=NAEK300"  // 부산
                + "&depPlandTime=" + today
                + "&_type=json&numOfRows=3&pageNo=1";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("=== HTTP 상태코드: " + response.statusCode() + " ===");
        System.out.println("=== 버스 raw 응답 ===");
        System.out.println(response.body());

        if (response.statusCode() == 200) {
            ObjectMapper om = new ObjectMapper();
            Map<String, Object> json = om.readValue(response.body(), Map.class);

            // data.go.kr 표준 응답 구조 파싱
            try {
                Map<String, Object> resp = (Map<String, Object>) json.get("response");
                Map<String, Object> body = (Map<String, Object>) resp.get("body");
                Map<String, Object> items = (Map<String, Object>) body.get("items");
                Object item = items.get("item");

                Map<String, Object> first = null;
                if (item instanceof List && !((List<?>) item).isEmpty()) {
                    first = (Map<String, Object>) ((List<?>) item).get(0);
                } else if (item instanceof Map) {
                    first = (Map<String, Object>) item;
                }

                if (first != null) {
                    System.out.println("\n=== 버스 첫 번째 아이템 키 목록 ===");
                    System.out.println(first.keySet());
                    System.out.println("\n=== 버스 첫 번째 아이템 값 ===");
                    first.forEach((k, v) -> System.out.printf("  %-30s = %s%n", k, v));
                }
            } catch (Exception e) {
                System.out.println("파싱 실패: " + e.getMessage());
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void 코레일_역코드_확인() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // 천안아산역 코드 확인 (현재 3900036 → 0건 반환 중)
        String[] searchTerms = {
            "%EC%B2%9C%EC%95%88%EC%95%84%EC%82%B0", // 천안아산
            "%EC%B2%9C%EC%95%88",                   // 천안
            "%EC%98%A4%EC%86%A1",                   // 오송
        };
        String[] labels = {"천안아산", "천안", "오송"};

        for (int i = 0; i < searchTerms.length; i++) {
            String url = "https://apis.data.go.kr/B551457/run/v2/codes2"
                    + "?serviceKey=" + SERVICE_KEY
                    + "&pageNo=1&numOfRows=10&returnType=JSON"
                    + "&cond%5Bstn_nm%3A%3AIKE%5D=" + searchTerms[i];

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("\n=== 역코드 조회: " + labels[i] + " ===");
            if (response.statusCode() == 200) {
                ObjectMapper om = new ObjectMapper();
                Map<String, Object> json = om.readValue(response.body(), Map.class);
                Object data = json.get("data");
                if (data instanceof List && !((List<?>) data).isEmpty()) {
                    for (Object item : (List<?>) data) {
                        Map<?,?> m = (Map<?,?>) item;
                        System.out.println("  역명=" + m.get("stn_nm") + " 코드=" + m.get("stn_cd"));
                    }
                } else {
                    // 구형 응답 구조
                    System.out.println(response.body().substring(0, Math.min(500, response.body().length())));
                }
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void 여객열차_운행정보_필드확인() throws Exception {
        String today = java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        // travelerTrainRunInfo2: 정차역별 열차출발/도착일시 제공
        String url = "https://apis.data.go.kr/B551457/run/v2/travelerTrainRunInfo2"
                + "?serviceKey=" + SERVICE_KEY
                + "&pageNo=1&numOfRows=5&returnType=JSON"
                + "&cond%5Brun_ymd%3A%3AEQ%5D=" + today;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("=== travelerTrainRunInfo2 HTTP: " + response.statusCode() + " ===");
        System.out.println(response.body().substring(0, Math.min(2000, response.body().length())));

        if (response.statusCode() == 200) {
            ObjectMapper om = new ObjectMapper();
            Map<String, Object> json = om.readValue(response.body(), Map.class);
            System.out.println("\n=== 최상위 키: " + json.keySet() + " ===");
            Object data = json.get("data");
            if (data instanceof List && !((List<?>) data).isEmpty()) {
                Map<String, Object> first = (Map<String, Object>) ((List<?>) data).get(0);
                System.out.println("=== 첫 번째 아이템 키: " + first.keySet() + " ===");
                first.forEach((k, v) -> System.out.printf("  %-35s = %s%n", k, v));
            }
        }

        // 열차번호 필터 테스트 (KTX 1호 = 001)
        String url2 = "https://apis.data.go.kr/B551457/run/v2/travelerTrainRunInfo2"
                + "?serviceKey=" + SERVICE_KEY
                + "&pageNo=1&numOfRows=20&returnType=JSON"
                + "&cond%5Brun_ymd%3A%3AEQ%5D=" + today
                + "&cond%5Btrain_no%3A%3AEQ%5D=001";

        HttpRequest req2 = HttpRequest.newBuilder().uri(URI.create(url2)).GET().build();
        HttpResponse<String> res2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
        System.out.println("\n=== KTX 001열차 정차역 정보 ===");
        System.out.println(res2.body().substring(0, Math.min(3000, res2.body().length())));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 서울_천안아산_KTX_조회() throws Exception {
        // 천안아산 역코드를 3900036으로 했을 때 0건이 나오므로 실제 코드 확인용
        String today = java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 천안아산역 코드 후보들 테스트
        String[] candidates = {"3900036", "3900035", "3900037", "0036", "1036"};
        HttpClient client = HttpClient.newHttpClient();

        for (String code : candidates) {
            String url = "https://apis.data.go.kr/B551457/run/v2/travelerTrainRunPlan2"
                    + "?serviceKey=" + SERVICE_KEY
                    + "&pageNo=1&numOfRows=3&returnType=JSON"
                    + "&cond%5Brun_ymd%3A%3AGTE%5D=" + today
                    + "&cond%5Brun_ymd%3A%3ALTE%5D=" + today
                    + "&cond%5Bdptre_stn_cd%3A%3AEQ%5D=3900023"
                    + "&cond%5Barvl_stn_cd%3A%3AEQ%5D=" + code;

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper om = new ObjectMapper();
            Map<String, Object> json = om.readValue(response.body(), Map.class);
            Object data = json.get("data");
            int count = 0;
            if (data instanceof List) count = ((List<?>) data).size();
            else {
                // 구형 응답
                try {
                    Map<?,?> resp = (Map<?,?>) json.get("response");
                    Map<?,?> body = (Map<?,?>) ((Map<?,?>)resp).get("body");
                    count = (Integer) body.get("totalCount");
                } catch (Exception ignored) {}
            }
            System.out.println("천안아산 코드=" + code + " → " + count + "건");
        }
    }
}
