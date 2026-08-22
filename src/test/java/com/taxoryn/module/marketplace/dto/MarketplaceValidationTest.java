package com.taxoryn.module.marketplace.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MarketplaceValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Validation: valid CreatePracticeProfileRequest has no constraint violations")
    void testValidCreateProfileRequest() {
        CreatePracticeProfileRequest request = CreatePracticeProfileRequest.builder()
                .displayName("Apex Corporate & Tax Advisors")
                .description("Expert tax consulting firm")
                .email("contact@apexadvisors.com")
                .phone("+919876543210")
                .websiteUrl("https://apexadvisors.com")
                .experienceYears(10)
                .build();

        Set<ConstraintViolation<CreatePracticeProfileRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Expected no violations for valid request: " + violations);
    }

    @Test
    @DisplayName("Validation: displayName must not be blank and must not exceed 255 characters")
    void testDisplayNameValidation() {
        // Blank
        CreatePracticeProfileRequest blankRequest = CreatePracticeProfileRequest.builder()
                .displayName("")
                .build();
        Set<ConstraintViolation<CreatePracticeProfileRequest>> violations = validator.validate(blankRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("displayName")));

        // Exceeds 255 chars
        CreatePracticeProfileRequest tooLongRequest = CreatePracticeProfileRequest.builder()
                .displayName("A".repeat(256))
                .build();
        Set<ConstraintViolation<CreatePracticeProfileRequest>> longViolations = validator.validate(tooLongRequest);
        assertFalse(longViolations.isEmpty());
        assertTrue(longViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("displayName")));
    }

    @Test
    @DisplayName("Validation: description/bio cannot exceed 5000 characters")
    void testDescriptionMaxLimit() {
        UpdateMarketplaceProfileRequest request = UpdateMarketplaceProfileRequest.builder()
                .displayName("Apex Advisors")
                .description("D".repeat(5001))
                .build();

        Set<ConstraintViolation<UpdateMarketplaceProfileRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));
    }

    @Test
    @DisplayName("Validation: email must follow valid email format")
    void testEmailValidation() {
        UpdateMarketplaceProfileRequest invalidEmail = UpdateMarketplaceProfileRequest.builder()
                .displayName("Apex Advisors")
                .email("invalid-email-string")
                .build();

        Set<ConstraintViolation<UpdateMarketplaceProfileRequest>> violations = validator.validate(invalidEmail);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));

        UpdateMarketplaceProfileRequest validEmail = UpdateMarketplaceProfileRequest.builder()
                .displayName("Apex Advisors")
                .email("info@apextax.com")
                .build();
        Set<ConstraintViolation<UpdateMarketplaceProfileRequest>> validViolations = validator.validate(validEmail);
        assertTrue(validViolations.isEmpty());
    }

    @Test
    @DisplayName("Validation: phone number adheres to project phone conventions")
    void testPhoneValidation() {
        UpdateMarketplaceProfileRequest invalidPhone = UpdateMarketplaceProfileRequest.builder()
                .displayName("Apex Advisors")
                .phone("abc-invalid-phone")
                .build();

        Set<ConstraintViolation<UpdateMarketplaceProfileRequest>> violations = validator.validate(invalidPhone);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("phone")));

        UpdateMarketplaceProfileRequest validPhone = UpdateMarketplaceProfileRequest.builder()
                .displayName("Apex Advisors")
                .phone("+91 98200 11223")
                .build();
        Set<ConstraintViolation<UpdateMarketplaceProfileRequest>> validViolations = validator.validate(validPhone);
        assertTrue(validViolations.isEmpty());
    }

    @Test
    @DisplayName("Validation: website URL must follow valid web URL format if supplied")
    void testWebsiteUrlValidation() {
        UpdateMarketplaceProfileRequest invalidUrl = UpdateMarketplaceProfileRequest.builder()
                .displayName("Apex Advisors")
                .websiteUrl("not a valid website url %%")
                .build();

        Set<ConstraintViolation<UpdateMarketplaceProfileRequest>> violations = validator.validate(invalidUrl);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("websiteUrl")));

        UpdateMarketplaceProfileRequest validUrl = UpdateMarketplaceProfileRequest.builder()
                .displayName("Apex Advisors")
                .websiteUrl("https://taxoryn.com/advisors/apex")
                .build();
        Set<ConstraintViolation<UpdateMarketplaceProfileRequest>> validViolations = validator.validate(validUrl);
        assertTrue(validViolations.isEmpty());
    }

    @Test
    @DisplayName("Validation: experienceYears must be non-negative and <= 100")
    void testExperienceYearsBounds() {
        // Negative
        UpdateMarketplaceProfileRequest negativeExp = UpdateMarketplaceProfileRequest.builder()
                .displayName("Apex Advisors")
                .experienceYears(-1)
                .build();

        Set<ConstraintViolation<UpdateMarketplaceProfileRequest>> violations = validator.validate(negativeExp);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("experienceYears")));

        // Exceeds 100
        UpdateMarketplaceProfileRequest excessiveExp = UpdateMarketplaceProfileRequest.builder()
                .displayName("Apex Advisors")
                .experienceYears(101)
                .build();

        Set<ConstraintViolation<UpdateMarketplaceProfileRequest>> excessiveViolations = validator.validate(excessiveExp);
        assertFalse(excessiveViolations.isEmpty());
        assertTrue(excessiveViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("experienceYears")));

        // Valid boundary
        UpdateMarketplaceProfileRequest validZeroExp = UpdateMarketplaceProfileRequest.builder()
                .displayName("Apex Advisors")
                .experienceYears(0)
                .build();
        assertTrue(validator.validate(validZeroExp).isEmpty());

        UpdateMarketplaceProfileRequest valid100Exp = UpdateMarketplaceProfileRequest.builder()
                .displayName("Apex Advisors")
                .experienceYears(100)
                .build();
        assertTrue(validator.validate(valid100Exp).isEmpty());
    }
}
