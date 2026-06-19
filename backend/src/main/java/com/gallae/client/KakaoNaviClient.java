package com.gallae.client;

import com.gallae.config.ExternalApiProperties;
import com.gallae.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoNaviClient {

    private final WebClient.Builder webClientBuilder;
    private final ExternalApiProperties properties;

    public Map<String, Object> fetchDirections(String origin, String destination) {
        String apiKey = properties.getKakao().getApiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("PLACEHOLDER")) {
            throw new ExternalApiException("카카오 API 키가 설정되지 않았습니다 (fallback 사용)");
        }
        try {
            log.debug("[KakaoNaviClient] fetchDirections: {} -> {}", origin, destination);
            // 실제 WebClient 호출 예시 구조:
            // return webClientBuilder.build()
            //     .get()
            //     .uri(properties.getKakao().getNaviBaseUrl() + "/v1/directions?origin={o}&destination={d}", origin, destination)
            //     .header("Authorization", "KakaoAK " + apiKey)
            //     .retrieve()
            //     .bodyToMono(Map.class)
            //     .block();
            throw new ExternalApiException("카카오 API 키 미설정 (fallback 사용)");
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[KakaoNaviClient] 호출 실패: {}", e.getMessage());
            throw new ExternalApiException("카카오 API 호출 실패", e);
        }
    }
}
