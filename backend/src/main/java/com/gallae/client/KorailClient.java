package com.gallae.client;

import com.gallae.config.ExternalApiProperties;
import com.gallae.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class KorailClient {

    private final WebClient.Builder webClientBuilder;
    private final ExternalApiProperties properties;

    public List<Map<String, Object>> fetchTrainCodes() {
        checkServiceKey();
        // GET {baseUrl}/codes2?serviceKey={key}&_type=json&resultType=json
        log.debug("[KorailClient] fetchTrainCodes");
        throw new ExternalApiException("코레일 API 키가 설정되지 않았습니다 (fallback 사용)");
    }

    public List<Map<String, Object>> fetchTrainRunPlan(String date, String depCode, String arrCode) {
        checkServiceKey();
        log.debug("[KorailClient] fetchTrainRunPlan: {} {} -> {}", date, depCode, arrCode);
        throw new ExternalApiException("코레일 API 키가 설정되지 않았습니다 (fallback 사용)");
    }

    public List<Map<String, Object>> fetchTrainRunInfo(String date, String trainNo) {
        checkServiceKey();
        log.debug("[KorailClient] fetchTrainRunInfo: {} {}", date, trainNo);
        throw new ExternalApiException("코레일 API 키가 설정되지 않았습니다 (fallback 사용)");
    }

    private void checkServiceKey() {
        String key = properties.getKorail().getServiceKey();
        if (key == null || key.isBlank() || key.equals("PLACEHOLDER")) {
            throw new ExternalApiException("코레일 API 키가 설정되지 않았습니다");
        }
    }
}
