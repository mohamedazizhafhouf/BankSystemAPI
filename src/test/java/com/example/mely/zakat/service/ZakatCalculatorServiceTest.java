package com.example.mely.zakat.service;

import com.example.mely.zakat.ZakatProperties;
import com.example.mely.zakat.dto.ZakatCalculationRequest;
import com.example.mely.zakat.dto.ZakatCalculationResponse;
import com.example.mely.zakat.dto.ZakatDefaultsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZakatCalculatorServiceTest {

    private ZakatProperties properties;
    private ZakatCalculatorService service;

    @BeforeEach
    void setUp() {
        properties = new ZakatProperties();
        properties.setGoldNisabGrams(new BigDecimal("87.48"));
        properties.setSilverNisabGrams(new BigDecimal("612.36"));
        properties.setDefaultGoldPricePerGram(new BigDecimal("10"));
        properties.setDefaultSilverPricePerGram(new BigDecimal("0.5"));
        properties.setZakatRate(new BigDecimal("0.025"));
        service = new ZakatCalculatorService(properties);
    }

    @Test
    void defaults_exposesConfiguredValues() {
        ZakatDefaultsResponse d = service.getDefaults();
        assertThat(d.getGoldNisabGrams()).isEqualByComparingTo("87.48");
        assertThat(d.getSilverNisabGrams()).isEqualByComparingTo("612.36");
        assertThat(d.getDefaultGoldPricePerGram()).isEqualByComparingTo("10");
        assertThat(d.getDefaultSilverPricePerGram()).isEqualByComparingTo("0.5");
        assertThat(d.getZakatRate()).isEqualByComparingTo("0.025");
    }

    @Test
    void nisabBoundary_meetsNisabWhenNetEqualsThreshold() {
        // nisab = 87.48 * 10 = 874.80
        ZakatCalculationRequest req = baseRequest();
        req.setCash(new BigDecimal("874.80"));
        req.setHawlAcknowledged(true);

        ZakatCalculationResponse r = service.calculate(req);
        assertThat(r.isMeetsNisab()).isTrue();
        assertThat(r.getNetZakatable()).isEqualByComparingTo("874.80");
        assertThat(r.getNisabThreshold()).isEqualByComparingTo("874.80");
        assertThat(r.getHypotheticalZakat()).isEqualByComparingTo("21.87");
        assertThat(r.getZakatDue()).isEqualByComparingTo("21.87");
    }

    @Test
    void nisabBoundary_justBelowNisab_zakatDueZeroEvenWithHawl() {
        ZakatCalculationRequest req = baseRequest();
        req.setCash(new BigDecimal("874.79"));
        req.setHawlAcknowledged(true);

        ZakatCalculationResponse r = service.calculate(req);
        assertThat(r.isMeetsNisab()).isFalse();
        assertThat(r.getHypotheticalZakat()).isEqualByComparingTo("21.87");
        assertThat(r.getZakatDue()).isEqualByComparingTo("0");
    }

    @Test
    void debtsReduceNet_andCanWipeZakat() {
        ZakatCalculationRequest req = baseRequest();
        req.setCash(new BigDecimal("5000"));
        req.setDebts(new BigDecimal("5000"));
        req.setHawlAcknowledged(true);

        ZakatCalculationResponse r = service.calculate(req);
        assertThat(r.getNetZakatable()).isEqualByComparingTo("0");
        assertThat(r.getHypotheticalZakat()).isEqualByComparingTo("0");
        assertThat(r.getZakatDue()).isEqualByComparingTo("0");
    }

    @Test
    void debtsExceedAssets_netClampedToZero_withWarning() {
        ZakatCalculationRequest req = baseRequest();
        req.setCash(new BigDecimal("100"));
        req.setDebts(new BigDecimal("150"));
        req.setHawlAcknowledged(true);

        ZakatCalculationResponse r = service.calculate(req);
        assertThat(r.getNetZakatable()).isEqualByComparingTo("0");
        assertThat(r.getWarnings()).isNotEmpty();
        assertThat(r.getHypotheticalZakat()).isEqualByComparingTo("0");
    }

    @Test
    void hawlNotAcknowledged_zakatDueZero_hypotheticalStillComputed() {
        ZakatCalculationRequest req = baseRequest();
        req.setCash(new BigDecimal("10000"));
        req.setHawlAcknowledged(false);

        ZakatCalculationResponse r = service.calculate(req);
        assertThat(r.isMeetsNisab()).isTrue();
        assertThat(r.getHypotheticalZakat()).isEqualByComparingTo("250.00");
        assertThat(r.getZakatDue()).isEqualByComparingTo("0");
    }

    @Test
    void goldGramsAddedAtRequestGoldPrice() {
        ZakatCalculationRequest req = baseRequest();
        req.setCash(new BigDecimal("800"));
        req.setGoldGrams(new BigDecimal("10"));
        req.setGoldPricePerGram(new BigDecimal("20"));
        req.setHawlAcknowledged(true);

        ZakatCalculationResponse r = service.calculate(req);
        // net = 800 + 200 = 1000, nisab = 87.48 * 20 = 1749.60 -> below nisab
        assertThat(r.getNetZakatable()).isEqualByComparingTo("1000.00");
        assertThat(r.isMeetsNisab()).isFalse();
    }

    @Test
    void invalidGoldPrice_throws() {
        properties.setDefaultGoldPricePerGram(BigDecimal.ZERO);
        ZakatCalculationRequest req = baseRequest();
        req.setCash(new BigDecimal("1000"));

        assertThatThrownBy(() -> service.calculate(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Gold price");
    }

    private static ZakatCalculationRequest baseRequest() {
        ZakatCalculationRequest req = new ZakatCalculationRequest();
        req.setGoldPricePerGram(new BigDecimal("10"));
        req.setSilverPricePerGram(new BigDecimal("0.5"));
        return req;
    }
}
