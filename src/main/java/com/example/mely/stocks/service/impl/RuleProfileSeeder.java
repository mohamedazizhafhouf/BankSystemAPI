package com.example.mely.stocks.service.impl;

import com.example.mely.stocks.model.RuleProfile;
import com.example.mely.stocks.repository.RuleProfileRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class RuleProfileSeeder {

    @Bean
    ApplicationRunner seedDefaultRuleProfile(RuleProfileRepository ruleProfileRepository) {
        return args -> {
            if (ruleProfileRepository.findFirstByActiveTrueOrderByCreatedAtAsc().isPresent()) {
                return;
            }

            RuleProfile profile = RuleProfile.builder()
                    .code("default-halal-stocks-v1")
                    .name("Default Halal Stocks Profile")
                    .version(1)
                    .active(true)
                    .thresholds(Map.of(
                            "debtRatioThreshold", "0.33",
                            "cashRatioThreshold", "0.33",
                            "nonCompliantIncomeThreshold", "0.05"
                    ))
                    .prohibitedKeywords(List.of(
                            "alcohol",
                            "gambling",
                            "casino",
                            "pork",
                            "interest",
                            "riba",
                            "conventional banking",
                            "adult entertainment",
                            "weapons"
                    ))
                    .build();
            ruleProfileRepository.save(profile);
        };
    }
}
