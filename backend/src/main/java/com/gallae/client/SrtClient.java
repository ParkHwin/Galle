package com.gallae.client;

import com.gallae.config.ExternalApiProperties;
import com.gallae.dto.FareDto;
import com.gallae.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SrtClient {

    private final WebClient.Builder webClientBuilder;
    private final ExternalApiProperties properties;

    public List<FareDto> fetchSrtFares(String from, String to) {
        try {
            String serviceKey = properties.getSrt().getServiceKey();
            if (serviceKey == null || serviceKey.isBlank() || serviceKey.equals("PLACEHOLDER")) {
                throw new ExternalApiException("SRT API 키가 설정되지 않았습니다");
            }
            // 실제 API 호출 로직 (키 발급 후 활성화)
            // WebClient 호출 예시 구조만 제공
            log.debug("[SrtClient] Fetching fares: {} -> {}", from, to);
            throw new ExternalApiException("SRT API 키가 설정되지 않았습니다 (fallback 사용)");
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[SrtClient] API 호출 실패: {}", e.getMessage());
            throw new ExternalApiException("SRT API 호출 실패", e);
        }
    }
}
