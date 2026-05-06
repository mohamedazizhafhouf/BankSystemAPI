package com.example.mely.service;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.mely.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor

public class ZakatSchedulerService {

    private final ZakatService zakatService;
    private final UserRepository userRepository;
    
    // Run daily at midnight to check for users who need zakat calculation
    @Scheduled(cron = "0 0 0 * * *")
    public void checkAndCalculateZakat() {
        log.info("Running scheduled Zakat calculation check");
        
        userRepository.findAll().stream()
                .filter(user -> user.getZakatHandling() != null && user.getZakatHandling())
                .forEach(user -> {
                    try {
                        zakatService.calculateZakat(user.getId());
                        log.info("Zakat calculated for user: {}", user.getId());
                    } catch (Exception e) {
                        log.error("Error calculating zakat for user {}: {}", user.getId(), e.getMessage());
                    }
                });
    }
    
    // Run weekly to update Nisab threshold based on gold prices
    @Scheduled(cron = "0 0 0 * * MON")
    public void updateNisabThreshold() {
        log.info("Updating Nisab threshold based on current gold prices");
        // Implementation would call external API to get current gold prices
        // Then update NisabConfiguration table
    }
    

    

}
