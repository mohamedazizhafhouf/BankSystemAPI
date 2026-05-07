package com.example.mely.zakat;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "mely.zakat")
public class ZakatProperties {

    /**
     * Gold niṣāb weight in grams (commonly cited ~87.48g).
     */
    private BigDecimal goldNisabGrams = new BigDecimal("87.48");

    /**
     * Silver niṣāb weight in grams (for reference / future use).
     */
    private BigDecimal silverNisabGrams = new BigDecimal("612.36");

    /**
     * Default gold price per gram in your display currency (user should override with market rate).
     */
    private BigDecimal defaultGoldPricePerGram = new BigDecimal("75");

    /**
     * Default silver price per gram in the same currency.
     */
    private BigDecimal defaultSilverPricePerGram = new BigDecimal("1");

    private BigDecimal zakatRate = new BigDecimal("0.025");
}
