package com.example.mely.zakat.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ZakatCalculationRequest {

    @DecimalMin(value = "0", inclusive = true, message = "cash must be >= 0")
    private BigDecimal cash;

    @DecimalMin(value = "0", inclusive = true, message = "bankSavings must be >= 0")
    private BigDecimal bankSavings;

    @DecimalMin(value = "0", inclusive = true, message = "receivables must be >= 0")
    private BigDecimal receivables;

    @DecimalMin(value = "0", inclusive = true, message = "tradeGoods must be >= 0")
    private BigDecimal tradeGoods;

    @DecimalMin(value = "0", inclusive = true, message = "investmentsZakatable must be >= 0")
    private BigDecimal investmentsZakatable;

    @DecimalMin(value = "0", inclusive = true, message = "otherAssets must be >= 0")
    private BigDecimal otherAssets;

    @DecimalMin(value = "0", inclusive = true, message = "debts must be >= 0")
    private BigDecimal debts;

    @DecimalMin(value = "0", inclusive = true, message = "goldGrams must be >= 0")
    private BigDecimal goldGrams;

    @DecimalMin(value = "0", inclusive = true, message = "silverGrams must be >= 0")
    private BigDecimal silverGrams;

    /**
     * Optional override; falls back to configured default.
     */
    @DecimalMin(value = "0", inclusive = false, message = "goldPricePerGram must be > 0 when set")
    private BigDecimal goldPricePerGram;

    @DecimalMin(value = "0", inclusive = false, message = "silverPricePerGram must be > 0 when set")
    private BigDecimal silverPricePerGram;

    /**
     * User confirms one lunar year (ḥawl) has passed on zakatable wealth.
     */
    private boolean hawlAcknowledged;
}
