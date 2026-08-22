package com.taxoryn.module.marketplace.service;

import com.taxoryn.module.marketplace.dto.CustomerProfileCompletenessDto;
import com.taxoryn.module.marketplace.entity.MarketplaceCustomerProfileEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerProfileCompletenessCalculatorTest {

    private CustomerProfileCompletenessCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CustomerProfileCompletenessCalculator();
    }

    @Test
    @DisplayName("Null profile returns 0% completeness with all items missing")
    void testNullProfile() {
        CustomerProfileCompletenessDto dto = calculator.calculate(null);
        assertThat(dto.getPercentage()).isEqualTo(0);
        assertThat(dto.getCompletedItems()).isEmpty();
        assertThat(dto.getMissingItems()).contains("Full Name", "Email Address", "Mobile Phone", "City", "State", "Pincode");
    }

    @Test
    @DisplayName("Minimal profile with name, email and phone calculates partial score")
    void testMinimalProfile() {
        MarketplaceCustomerProfileEntity profile = MarketplaceCustomerProfileEntity.builder()
                .userId(UUID.randomUUID())
                .firstName("Ananya")
                .displayName("Ananya Iyer")
                .email("ananya@example.com")
                .phone("9876543210")
                .preferredLanguage("English")
                .build();

        CustomerProfileCompletenessDto dto = calculator.calculate(profile);
        // Name(15) + Email(15) + Phone(15) + Lang(10) = 55%
        assertThat(dto.getPercentage()).isEqualTo(55);
        assertThat(dto.getCompletedItems()).contains("Full Name", "Email Address", "Mobile Phone", "Language Preference");
        assertThat(dto.getMissingItems()).contains("City", "State", "Pincode");
    }

    @Test
    @DisplayName("Fully populated profile achieves 100% completeness")
    void testFullProfile() {
        MarketplaceCustomerProfileEntity profile = MarketplaceCustomerProfileEntity.builder()
                .userId(UUID.randomUUID())
                .firstName("Vikram")
                .lastName("Mehta")
                .displayName("Vikram Mehta")
                .email("vikram@example.com")
                .phone("9123456789")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .preferredLanguage("English")
                .build();

        CustomerProfileCompletenessDto dto = calculator.calculate(profile);
        assertThat(dto.getPercentage()).isEqualTo(100);
        assertThat(dto.getMissingItems()).isEmpty();
        assertThat(dto.getCompletedItems()).containsExactlyInAnyOrder(
                "Full Name", "Email Address", "Mobile Phone", "City", "State", "Pincode", "Language Preference"
        );
    }
}
