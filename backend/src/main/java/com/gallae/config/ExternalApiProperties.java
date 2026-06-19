package com.gallae.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "external")
public class ExternalApiProperties {

    private Srt srt = new Srt();
    private Korail korail = new Korail();
    private Bus bus = new Bus();
    private Kakao kakao = new Kakao();

    @Data
    public static class Srt {
        private String baseUrl;
        private String serviceKey;
    }

    @Data
    public static class Korail {
        private String baseUrl;
        private String serviceKey;
    }

    @Data
    public static class Bus {
        private String baseUrl;
        private String serviceKey;
    }

    @Data
    public static class Kakao {
        private String naviBaseUrl;
        private String apiKey;
    }
}
