package com.example.mely.zakat.service;

import com.example.mely.zakat.ZakatProperties;
import com.example.mely.zakat.dto.ZakatBreakdownLineResponse;
import com.example.mely.zakat.dto.ZakatCalculationRequest;
import com.example.mely.zakat.dto.ZakatCalculationResponse;
import com.example.mely.zakat.dto.ZakatDefaultsResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class ZakatCalculatorService {

    public static final String DISCLAIMER = """
            This tool provides a simplified Zakat estimate for educational purposes only. It is not a fatwa \
            or formal religious ruling. Zakat rules vary by school of thought, asset type, debt timing, and \
            personal circumstances. Always verify with a qualified scholar or trusted charity before paying \
            Zakat. Mely uses a gold-based niṣāb threshold in this version (configured grams × your gold price per gram).""";

    private final ZakatProperties properties;

    public ZakatCalculatorService(ZakatProperties properties) {
        this.properties = properties;
    }

    public ZakatDefaultsResponse getDefaults() {
        return ZakatDefaultsResponse.builder()
                .goldNisabGrams(properties.getGoldNisabGrams())
                .silverNisabGrams(properties.getSilverNisabGrams())
                .defaultGoldPricePerGram(properties.getDefaultGoldPricePerGram())
                .defaultSilverPricePerGram(properties.getDefaultSilverPricePerGram())
                .zakatRate(properties.getZakatRate())
                .build();
    }

    public ZakatCalculationResponse calculate(ZakatCalculationRequest request) {
        BigDecimal goldPrice = request.getGoldPricePerGram() != null
                ? request.getGoldPricePerGram()
                : properties.getDefaultGoldPricePerGram();
        BigDecimal silverPrice = request.getSilverPricePerGram() != null
                ? request.getSilverPricePerGram()
                : properties.getDefaultSilverPricePerGram();

        if (goldPrice == null || goldPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Gold price per gram must be positive (set it in the request or configure mely.zakat.default-gold-price-per-gram).");
        }
        if (silverPrice == null || silverPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Silver price per gram must be positive (set it in the request or configure mely.zakat.default-silver-price-per-gram).");
        }

        BigDecimal cash = zeroIfNull(request.getCash());
        BigDecimal bankSavings = zeroIfNull(request.getBankSavings());
        BigDecimal receivables = zeroIfNull(request.getReceivables());
        BigDecimal tradeGoods = zeroIfNull(request.getTradeGoods());
        BigDecimal investments = zeroIfNull(request.getInvestmentsZakatable());
        BigDecimal other = zeroIfNull(request.getOtherAssets());
        BigDecimal debts = zeroIfNull(request.getDebts());
        BigDecimal goldGrams = zeroIfNull(request.getGoldGrams());
        BigDecimal silverGrams = zeroIfNull(request.getSilverGrams());

        BigDecimal goldValue = goldGrams.multiply(goldPrice);
        BigDecimal silverValue = silverGrams.multiply(silverPrice);

        BigDecimal assetTotal = cash
                .add(bankSavings)
                .add(receivables)
                .add(tradeGoods)
                .add(investments)
                .add(other)
                .add(goldValue)
                .add(silverValue);

        List<String> warnings = new ArrayList<>();
        BigDecimal netBeforeClamp = assetTotal.subtract(debts);
        BigDecimal netZakatable = netBeforeClamp;
        if (netBeforeClamp.compareTo(BigDecimal.ZERO) < 0) {
            warnings.add("Debts exceed declared assets; net zakatable wealth is treated as zero for this estimate.");
            netZakatable = BigDecimal.ZERO;
        }

        netZakatable = netZakatable.setScale(2, RoundingMode.HALF_UP);

        BigDecimal nisabThreshold = properties.getGoldNisabGrams()
                .multiply(goldPrice)
                .setScale(2, RoundingMode.HALF_UP);

        boolean meetsNisab = netZakatable.compareTo(nisabThreshold) >= 0;

        BigDecimal rate = properties.getZakatRate();
        BigDecimal hypotheticalZakat = BigDecimal.ZERO;
        if (netZakatable.compareTo(BigDecimal.ZERO) > 0) {
            hypotheticalZakat = netZakatable.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal zakatDue = BigDecimal.ZERO;
        if (request.isHawlAcknowledged() && meetsNisab) {
            zakatDue = hypotheticalZakat;
        }

        List<ZakatBreakdownLineResponse> breakdown = new ArrayList<>();
        addIfPositive(breakdown, "Cash", cash);
        addIfPositive(breakdown, "Bank savings", bankSavings);
        addIfPositive(breakdown, "Receivables", receivables);
        addIfPositive(breakdown, "Trade / business goods", tradeGoods);
        addIfPositive(breakdown, "Investments (zakatable portion)", investments);
        addIfPositive(breakdown, "Other zakatable assets", other);
        if (goldGrams.compareTo(BigDecimal.ZERO) > 0) {
            breakdown.add(ZakatBreakdownLineResponse.builder()
                    .label("Gold (value at your price per gram)")
                    .amount(goldValue.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        if (silverGrams.compareTo(BigDecimal.ZERO) > 0) {
            breakdown.add(ZakatBreakdownLineResponse.builder()
                    .label("Silver (value at your price per gram)")
                    .amount(silverValue.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        breakdown.add(ZakatBreakdownLineResponse.builder()
                .label("Subtotal assets")
                .amount(assetTotal.setScale(2, RoundingMode.HALF_UP))
                .build());
        if (debts.compareTo(BigDecimal.ZERO) > 0) {
            breakdown.add(ZakatBreakdownLineResponse.builder()
                    .label("Deductible debts")
                    .amount(debts.negate().setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        breakdown.add(ZakatBreakdownLineResponse.builder()
                .label("Net zakatable wealth")
                .amount(netZakatable)
                .build());
        breakdown.add(ZakatBreakdownLineResponse.builder()
                .label("Niṣāb threshold (gold " + properties.getGoldNisabGrams().stripTrailingZeros().toPlainString() + " g × gold price)")
                .amount(nisabThreshold)
                .build());

        return ZakatCalculationResponse.builder()
                .netZakatable(netZakatable)
                .nisabThreshold(nisabThreshold)
                .meetsNisab(meetsNisab)
                .zakatDue(zakatDue)
                .hypotheticalZakat(hypotheticalZakat)
                .rate(rate)
                .breakdown(breakdown)
                .disclaimer(DISCLAIMER)
                .warnings(warnings)
                .build();
    }

    private static BigDecimal zeroIfNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static void addIfPositive(List<ZakatBreakdownLineResponse> breakdown, String label, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            breakdown.add(ZakatBreakdownLineResponse.builder()
                    .label(label)
                    .amount(amount.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
    }
}
