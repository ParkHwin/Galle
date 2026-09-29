package com.gallae.config;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    /**
     * prototype으로 선언해야 각 Client가 독립적인 Builder를 가진다.
     * singleton이면 SrtClient가 .baseUrl() 호출 시 공유 Builder를 오염시켜
     * 다른 Client URL에 SRT 경로가 끼어드는 버그 발생.
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
