package com.example.mely.dto.request;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class NisabUpdateRequest {

    private BigDecimal goldValuePerGram;
    private BigDecimal silverValuePerGram;
    private String currencyCode;
}
