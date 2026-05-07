package com.example.mely.config;

import com.example.mely.currency.CurrencylayerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CurrencylayerProperties.class)
public class CurrencyModuleConfig {

    @Bean
    public RestClient currencylayerRestClient(CurrencylayerProperties properties) {
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        String key = properties.resolveApiKey();
        if (!key.isEmpty()) {
            builder.defaultHeader("apikey", key);
        }
        return builder.build();
    }
}
