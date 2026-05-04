package com.example.mely.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StockStorageProperties.class)
public class StockModuleConfig {
}
