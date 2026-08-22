package com.taxoryn.module.marketplace.service;

import com.taxoryn.module.marketplace.dto.CustomerProfileCompletenessDto;
import com.taxoryn.module.marketplace.entity.MarketplaceCustomerProfileEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class CustomerProfileCompletenessCalculator {

    public CustomerProfileCompletenessDto calculate(MarketplaceCustomerProfileEntity profile) {
        if (profile == null) {
            return CustomerProfileCompletenessDto.builder()
                    .percentage(0)
                    .completedItems(List.of())
                    .missingItems(List.of("Full Name", "Email Address", "Mobile Phone", "City", "State", "Pincode", "Language Preference"))
                    .build();
        }

        List<String> completed = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        int score = 0;

        // 1. Full Name (15%)
        if (StringUtils.hasText(profile.getFirstName()) && StringUtils.hasText(profile.getDisplayName())) {
            completed.add("Full Name");
            score += 15;
        } else {
            missing.add("Full Name");
        }

        // 2. Email Address (15%)
        if (StringUtils.hasText(profile.getEmail())) {
            completed.add("Email Address");
            score += 15;
        } else {
            missing.add("Email Address");
        }

        // 3. Mobile Phone (15%)
        if (StringUtils.hasText(profile.getPhone())) {
            completed.add("Mobile Phone");
            score += 15;
        } else {
            missing.add("Mobile Phone");
        }

        // 4. City (15%)
        if (StringUtils.hasText(profile.getCity())) {
            completed.add("City");
            score += 15;
        } else {
            missing.add("City");
        }

        // 5. State (15%)
        if (StringUtils.hasText(profile.getState())) {
            completed.add("State");
            score += 15;
        } else {
            missing.add("State");
        }

        // 6. Pincode (15%)
        if (StringUtils.hasText(profile.getPincode())) {
            completed.add("Pincode");
            score += 15;
        } else {
            missing.add("Pincode");
        }

        // 7. Language Preference (10%)
        if (StringUtils.hasText(profile.getPreferredLanguage())) {
            completed.add("Language Preference");
            score += 10;
        } else {
            missing.add("Language Preference");
        }

        return CustomerProfileCompletenessDto.builder()
                .percentage(Math.min(score, 100))
                .completedItems(completed)
                .missingItems(missing)
                .build();
    }
}
