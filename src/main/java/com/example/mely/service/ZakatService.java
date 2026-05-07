package com.example.mely.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.mely.dto.request.ZakatAssetRequest;
import com.example.mely.dto.response.ZakatCalculationResponse;
import com.example.mely.model.Enums.ZakatStatus;
import com.example.mely.model.HawlTracking;
import com.example.mely.model.NisabConfiguration;
import com.example.mely.model.User;
import com.example.mely.model.ZakatAsset;
import com.example.mely.model.ZakatCalculation;
import com.example.mely.repository.HawlTrackingRepository;
import com.example.mely.repository.NisabConfigurationRepository;
import com.example.mely.repository.UserRepository;
import com.example.mely.repository.ZakatAssetRepository;
import com.example.mely.repository.ZakatCalculationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZakatService {

    private final ZakatAssetRepository zakatAssetRepository;
    private final ZakatCalculationRepository zakatCalculationRepository;
    private final NisabConfigurationRepository nisabConfigurationRepository;
    private final HawlTrackingRepository hawlTrackingRepository;
    private final UserRepository userRepository;
    
    private static final BigDecimal ZAKAT_RATE = new BigDecimal("0.025"); // 2.5%
    private static final BigDecimal GOLD_NISAB_GRAMS = new BigDecimal("85");
    private static final BigDecimal SILVER_NISAB_GRAMS = new BigDecimal("595");
    
    @Transactional
    public ZakatAsset addZakatAsset(ZakatAssetRequest request) {
        ZakatAsset asset = ZakatAsset.builder()
                .userId(request.getUserId())
                .assetType(request.getAssetType())
                .amount(request.getAmount())
                .value(request.getValue())
                .currencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "SAR")
                .description(request.getDescription())
                .acquisitionDate(request.getAcquisitionDate() != null ? 
                    request.getAcquisitionDate() : LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .isActive(true)
                .weight(request.getWeight())
                .purity(request.getPurity())
                .annualReturnRate(request.getAnnualReturnRate())
                .build();
                ZakatAsset savedAsset = zakatAssetRepository.save(asset);
        
        // Update Hawl tracking if this is the first asset
        initializeHawlTracking(request.getUserId());
        
        return savedAsset;
    }
    
    @Transactional
    public void initializeHawlTracking(UUID userId) {
        HawlTracking existing = hawlTrackingRepository.findByUserId(userId).orElse(null);
        
        if (existing == null) {
            LocalDate today = LocalDate.now();
            HawlTracking hawlTracking = HawlTracking.builder()
                    .userId(userId)
                    .hawlStartDate(today)
                    .hawlEndDate(today.plusYears(1))
                    .isHawlCompleted(false)
                    .daysRemaining(365)
                    .previousYearZakatPaid(false)
                    .build();
            
            hawlTrackingRepository.save(hawlTracking);
        }
    }

    @Transactional
    public ZakatCalculationResponse calculateZakat(UUID userId) {
        log.info("Calculating Zakat for user: {}", userId);
        
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if user opted for zakat handling
        if (user.getZakatHandling() == null || !user.getZakatHandling()) {
            return ZakatCalculationResponse.builder()
                    .userId(userId.toString())
                    .message("User has not opted for automatic Zakat calculation")
                    .build();
        }
        
        // Get or create Hawl tracking
        HawlTracking hawlTracking = hawlTrackingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    initializeHawlTracking(userId);
                    return hawlTrackingRepository.findByUserId(userId).get();
                });
        
        // Get active assets
        List<ZakatAsset> assets = zakatAssetRepository.findByUserIdAndIsActiveTrue(userId);
        
        if (assets.isEmpty()) {

            return ZakatCalculationResponse.builder()
                    .userId(userId.toString())
                    .calculationDate(LocalDate.now())
                    .totalAssets(BigDecimal.ZERO)
                    .message("No active assets found for Zakat calculation")
                    .build();
        }
        
        // Calculate total assets and breakdown
        Map<String, BigDecimal> assetBreakdown = new HashMap<>();
        BigDecimal totalAssets = BigDecimal.ZERO;
        
        for (ZakatAsset asset : assets) {
            BigDecimal assetValue = calculateAssetValue(asset);
            totalAssets = totalAssets.add(assetValue);
            
            String assetTypeName = asset.getAssetType().toString();
            assetBreakdown.merge(assetTypeName, assetValue, BigDecimal::add);
        }
        
        // Get current Nisab threshold
        BigDecimal nisabThreshold = getCurrentNisabThreshold();
        String nisabCurrency = "SAR";
        
        // Check if assets meet Hawl requirement
        boolean hawlCompleted = isHawlCompleted(hawlTracking);

        boolean aboveNisab = totalAssets.compareTo(nisabThreshold) >= 0;
        
        ZakatStatus status;
        BigDecimal zakatDue = BigDecimal.ZERO;
        
        if (!hawlCompleted) {
            status = ZakatStatus.EXEMPT_HAWL_NOT_MET;
        } else if (!aboveNisab) {
            status = ZakatStatus.EXEMPT_BELOW_NISAB;
        } else {
            // Calculate Zakat due (2.5% of total assets)
            zakatDue = totalAssets.multiply(ZAKAT_RATE).setScale(2, RoundingMode.HALF_UP);
            status = ZakatStatus.DUE;
        }
        
        // Save calculation
        ZakatCalculation calculation = ZakatCalculation.builder()
                .userId(userId)
                .calculationDate(LocalDate.now())
                .hawlStartDate(hawlTracking.getHawlStartDate())
                .hawlEndDate(hawlTracking.getHawlEndDate())
                .totalAssets(totalAssets)
                .nisabThreshold(nisabThreshold)
                .nisabCurrency(nisabCurrency)
                .zakatDueAmount(zakatDue)
                .zakatRate(ZAKAT_RATE)
                .zakatStatus(status)
                .calculatedAt(LocalDateTime.now())
                .assetBreakdown(assetBreakdown.toString())
                .build();
        
        zakatCalculationRepository.save(calculation);
        
        // Update last calculation date
        hawlTracking.setLastCalculationDate(LocalDate.now());
        hawlTrackingRepository.save(hawlTracking);
        
        return ZakatCalculationResponse.builder()
                .userId(userId.toString())
                .calculationDate(LocalDate.now())
                .hawlStartDate(hawlTracking.getHawlStartDate())
                .hawlEndDate(hawlTracking.getHawlEndDate())
                .totalAssets(totalAssets)
                .nisabThreshold(nisabThreshold)
                .nisabCurrency(nisabCurrency)
                .nisabInSAR(convertToSAR(nisabThreshold))
                .isAboveNisab(aboveNisab)
                .zakatDueAmount(zakatDue)
                .zakatRate(ZAKAT_RATE)
                                .zakatStatus(status.toString())
                .assetBreakdown(assetBreakdown)
                .message(getStatusMessage(status, zakatDue, totalAssets, nisabThreshold))
                .build();
    }
    
    private BigDecimal calculateAssetValue(ZakatAsset asset) {
        // For assets with specific calculations
        switch (asset.getAssetType()) {
            case GOLD:
                if (asset.getWeight() != null && asset.getPurity() != null) {
                    // Calculate pure gold weight
                    BigDecimal pureGoldWeight = asset.getWeight()
                            .multiply(asset.getPurity().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                    // Assume gold price per gram (would come from market data)
                    BigDecimal goldPricePerGram = getGoldPricePerGram();
                    return pureGoldWeight.multiply(goldPricePerGram);
                }
                return asset.getValue();
                
            case SILVER:
                if (asset.getWeight() != null) {
                    BigDecimal silverPricePerGram = getSilverPricePerGram();
                    return asset.getWeight().multiply(silverPricePerGram);
                }
                return asset.getValue();

                 case INVESTMENT_STOCKS:
            case INVESTMENT_MUTUAL_FUNDS:
                // For investments, use current market value
                return asset.getValue();
                
            case BUSINESS_INVENTORY:
                // For business inventory, calculate based on cost or market value
                return asset.getValue();
                
            default:
                return asset.getValue();
        }
    }
    
    private BigDecimal getCurrentNisabThreshold() {
        // Get active Nisab configuration
        NisabConfiguration config = nisabConfigurationRepository
                .findActiveConfigurationAtDate(LocalDateTime.now())
                .orElse(null);
        
        if (config != null) {
             return config.getValue();
        }
        
        // Default calculation using gold price
        BigDecimal goldPricePerGram = getGoldPricePerGram();
        return goldPricePerGram.multiply(GOLD_NISAB_GRAMS);
    }
    
    private BigDecimal getGoldPricePerGram() {
        // In production, this would call an external API for current gold prices
        // For now, return a default value (SAR per gram)
        return new BigDecimal("250.00"); // Example: 250 SAR per gram
    }
    
    private BigDecimal getSilverPricePerGram() {
        // In production, this would call an external API
        return new BigDecimal("3.00"); // Example: 3 SAR per gram
    }
    
    private BigDecimal convertToSAR(BigDecimal amount) {
        // Convert to SAR if needed (simplified)
        return amount;
    }
    private boolean isHawlCompleted(HawlTracking hawlTracking) {
        LocalDate today = LocalDate.now();
        
        if (hawlTracking.getIsHawlCompleted()) {
            return true;
        }
        
        // Check if one lunar year (355 days) has passed
        long daysSinceStart = ChronoUnit.DAYS.between(hawlTracking.getHawlStartDate(), today);
        
        if (daysSinceStart >= 355) {
            hawlTracking.setIsHawlCompleted(true);
            hawlTracking.setHawlEndDate(today);
            hawlTrackingRepository.save(hawlTracking);
            return true;
        }
        
        // Update days remaining
        int daysRemaining = 355 - (int) daysSinceStart;
        hawlTracking.setDaysRemaining(daysRemaining);
        hawlTrackingRepository.save(hawlTracking);
        
        return false;
    }
     private String getStatusMessage(ZakatStatus status, BigDecimal zakatDue, 
                                     BigDecimal totalAssets, BigDecimal nisab) {
        switch (status) {
            case EXEMPT_HAWL_NOT_MET:
                return "Hawl (one lunar year) not yet completed. Zakat is not due at this time.";
            case EXEMPT_BELOW_NISAB:
                return String.format("Total assets (%.2f) are below the Nisab threshold (%.2f). Zakat is not due.",
                        totalAssets, nisab);
            case DUE:
                return String.format("Zakat of %.2f is due on total assets of %.2f.", zakatDue, totalAssets);
            case PAID:
                return "Zakat has been paid for this period.";
            default:
                return "Zakat calculation completed.";
        }
    }

    public List<ZakatAsset> listActiveZakatAssets(UUID userId) {
        return zakatAssetRepository.findByUserIdAndIsActiveTrue(userId);
    }

    @Transactional
    public ZakatAsset addZakatAssetForUser(UUID userId, ZakatAssetRequest request) {
        request.setUserId(userId);
        return addZakatAsset(request);
    }

    @Transactional
    public ZakatAsset updateZakatAssetForUser(UUID userId, UUID assetId, ZakatAssetRequest request) {
        ZakatAsset asset = zakatAssetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
        if (!asset.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found");
        }
        asset.setAssetType(request.getAssetType());
        asset.setAmount(request.getAmount());
        asset.setValue(request.getValue());
        asset.setCurrencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "SAR");
        asset.setDescription(request.getDescription());
        if (request.getAcquisitionDate() != null) {
            asset.setAcquisitionDate(request.getAcquisitionDate());
        }
        asset.setWeight(request.getWeight());
        asset.setPurity(request.getPurity());
        asset.setAnnualReturnRate(request.getAnnualReturnRate());
        asset.setLastUpdated(LocalDateTime.now());
        return zakatAssetRepository.save(asset);
    }

    @Transactional
    public void deactivateZakatAssetForUser(UUID userId, UUID assetId) {
        ZakatAsset asset = zakatAssetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
        if (!asset.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found");
        }
        asset.setIsActive(false);
        asset.setLastUpdated(LocalDateTime.now());
        zakatAssetRepository.save(asset);
    }

    public Optional<HawlTracking> findHawlTracking(UUID userId) {
        return hawlTrackingRepository.findByUserId(userId);
    }

}
