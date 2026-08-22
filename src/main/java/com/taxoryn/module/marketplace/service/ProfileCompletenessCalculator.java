package com.taxoryn.module.marketplace.service;

import com.taxoryn.module.marketplace.dto.ProfileCompletenessDto;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Extensible Profile Completeness Calculator for Taxoryn Practice Marketplace.
 * Evaluates profile completeness from available PracticeProfile & Organization attributes.
 * Designed with a pluggable rule pipeline to seamlessly support future modules (Location, Service Catalog, etc.).
 */
@Component
public class ProfileCompletenessCalculator {

    @Getter
    @Builder
    public static class CompletenessRule {
        private final String name;
        private final int weight;
        private final BiPredicate<MarketplaceProfileEntity, OrganizationEntity> predicate;
    }

    private final List<CompletenessRule> rules = new ArrayList<>();

    public ProfileCompletenessCalculator() {
        registerDefaultRules();
    }

    private void registerDefaultRules() {
        // 1. Practice Name (Weight: 20%)
        rules.add(CompletenessRule.builder()
                .name("Practice name")
                .weight(20)
                .predicate((profile, org) -> (profile != null && StringUtils.hasText(profile.getDisplayName()))
                        || (org != null && StringUtils.hasText(org.getName())))
                .build());

        // 2. Description (Weight: 20%)
        rules.add(CompletenessRule.builder()
                .name("Description")
                .weight(20)
                .predicate((profile, org) -> profile != null && StringUtils.hasText(profile.getBio())
                        && profile.getBio().trim().length() >= 20)
                .build());

        // 3. Contact Phone (Weight: 15%)
        rules.add(CompletenessRule.builder()
                .name("Phone")
                .weight(15)
                .predicate((profile, org) -> (profile != null && StringUtils.hasText(profile.getPhone()))
                        || (org != null && StringUtils.hasText(org.getPhone())))
                .build());

        // 4. Contact Email (Weight: 15%)
        rules.add(CompletenessRule.builder()
                .name("Email")
                .weight(15)
                .predicate((profile, org) -> (profile != null && StringUtils.hasText(profile.getEmail()))
                        || (org != null && StringUtils.hasText(org.getEmail())))
                .build());

        // 5. Location (Weight: 10%)
        rules.add(CompletenessRule.builder()
                .name("Location")
                .weight(10)
                .predicate((profile, org) -> {
                    String city = profile != null && StringUtils.hasText(profile.getCity()) ? profile.getCity() : (org != null ? org.getCity() : null);
                    String state = profile != null && StringUtils.hasText(profile.getState()) ? profile.getState() : (org != null ? org.getState() : null);
                    return StringUtils.hasText(city) && StringUtils.hasText(state);
                })
                .build());

        // 6. Experience (Weight: 10%)
        rules.add(CompletenessRule.builder()
                .name("Experience")
                .weight(10)
                .predicate((profile, org) -> profile != null && profile.getExperienceYears() != null && profile.getProfessionalType() != null)
                .build());

        // 7. Website (Weight: 10%)
        rules.add(CompletenessRule.builder()
                .name("Website")
                .weight(10)
                .predicate((profile, org) -> profile != null && StringUtils.hasText(profile.getWebsiteUrl()))
                .build());
    }

    /**
     * Calculates the overall profile completeness metrics.
     *
     * @param profile      the practice's public marketplace profile entity
     * @param organization the practice's organization tenant entity
     * @return ProfileCompletenessDto containing percentage, completedItems, and missingItems
     */
    public ProfileCompletenessDto calculate(MarketplaceProfileEntity profile, OrganizationEntity organization) {
        int earnedScore = 0;
        int totalWeight = 0;
        List<String> completedItems = new ArrayList<>();
        List<String> missingItems = new ArrayList<>();

        for (CompletenessRule rule : rules) {
            totalWeight += rule.getWeight();
            boolean satisfied = false;
            try {
                satisfied = rule.getPredicate().test(profile, organization);
            } catch (Exception ignored) {
                satisfied = false;
            }

            if (satisfied) {
                earnedScore += rule.getWeight();
                completedItems.add(rule.getName());
            } else {
                missingItems.add(rule.getName());
            }
        }

        int percentage = totalWeight > 0 ? (int) Math.round(((double) earnedScore / totalWeight) * 100.0) : 0;
        percentage = Math.min(100, Math.max(0, percentage));

        return ProfileCompletenessDto.builder()
                .percentage(percentage)
                .completedItems(completedItems)
                .missingItems(missingItems)
                .build();
    }
}
