package com.example.mely.config;

import com.example.mely.metals.GoldApiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GoldApiProperties.class)
public class MetalsModuleConfig {

    @Bean
    public RestClient goldApiRestClient(GoldApiProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    /**
     * Ensures an {@link ObjectMapper} exists for {@link com.example.mely.metals.service.GoldApiService}
     * if Jackson auto-configuration did not register one.
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
