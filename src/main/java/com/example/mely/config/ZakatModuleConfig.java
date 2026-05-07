package com.example.mely.config;

import com.example.mely.zakat.ZakatProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ZakatProperties.class)
public class ZakatModuleConfig {
}
