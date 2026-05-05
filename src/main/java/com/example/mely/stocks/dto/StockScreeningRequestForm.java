package com.example.mely.stocks.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class StockScreeningRequestForm {
    @NotBlank(message = "companyName is required")
    private String companyName;

    private String ticker;

    @NotBlank(message = "countryOrMarket is required")
    private String countryOrMarket;

    @NotBlank(message = "currency is required")
    private String currency;

    @NotBlank(message = "businessDescription is required")
    private String businessDescription;

    private String sector;
    private String fiscalYear;
    private String reportingPeriod;
    private String mode;

    private BigDecimal interestBearingDebt;
    private BigDecimal cashAndInterestBearingAssets;
    private BigDecimal totalAssetsOrMarketCap;
    private BigDecimal totalRevenue;
    private BigDecimal nonCompliantIncome;
    private BigDecimal sharesOwned;
    private BigDecimal investedAmount;

    private List<MultipartFile> documents = new ArrayList<>();
}
