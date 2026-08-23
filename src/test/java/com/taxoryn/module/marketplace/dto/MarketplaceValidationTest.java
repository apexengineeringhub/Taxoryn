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

    @Test
    @DisplayName("Validation: valid CreatePracticeLocationRequest passes all constraints")
    void testValidCreatePracticeLocationRequest() {
        CreatePracticeLocationRequest request = CreatePracticeLocationRequest.builder()
                .locationName("Bengaluru Head Office")
                .addressLine1("Prestige Meridian II, MG Road")
                .addressLine2("4th Floor")
                .city("Bengaluru")
                .district("Bengaluru Urban")
                .state("Karnataka")
                .stateCode("KA")
                .country("India")
                .countryCode("IN")
                .pincode("560001")
                .latitude(new java.math.BigDecimal("12.971600"))
                .longitude(new java.math.BigDecimal("77.594600"))
                .isPrimary(true)
                .build();

        Set<ConstraintViolation<CreatePracticeLocationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Expected no violations: " + violations);
    }

    @Test
    @DisplayName("Validation: location mandatory fields (locationName, addressLine1, city, state, pincode) cannot be blank")
    void testLocationMandatoryFieldsValidation() {
        CreatePracticeLocationRequest emptyRequest = CreatePracticeLocationRequest.builder()
                .locationName("")
                .addressLine1("")
                .city("")
                .state("")
                .pincode("")
                .build();

        Set<ConstraintViolation<CreatePracticeLocationRequest>> violations = validator.validate(emptyRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("locationName")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("addressLine1")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("city")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("state")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("pincode")));
    }

    @Test
    @DisplayName("Validation: pincode must follow valid Indian 6-digit postal PIN code format")
    void testLocationPincodeFormatValidation() {
        CreatePracticeLocationRequest invalidPin = CreatePracticeLocationRequest.builder()
                .locationName("Branch Office")
                .addressLine1("Main Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("012345") // Starts with 0
                .build();
        assertFalse(validator.validate(invalidPin).isEmpty());

        CreatePracticeLocationRequest shortPin = CreatePracticeLocationRequest.builder()
                .locationName("Branch Office")
                .addressLine1("Main Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("40001") // 5 digits
                .build();
        assertFalse(validator.validate(shortPin).isEmpty());

        CreatePracticeLocationRequest validPin = CreatePracticeLocationRequest.builder()
                .locationName("Branch Office")
                .addressLine1("Main Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001") // 6 digits
                .build();
        assertTrue(validator.validate(validPin).isEmpty());
    }

    @Test
    @DisplayName("Validation: geographic coordinates pair must be provided together")
    void testLocationCoordinatesPairValidation() {
        // Only latitude provided
        CreatePracticeLocationRequest onlyLat = CreatePracticeLocationRequest.builder()
                .locationName("Branch Office")
                .addressLine1("Main Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .latitude(new java.math.BigDecimal("19.076000"))
                .longitude(null)
                .build();
        assertFalse(validator.validate(onlyLat).isEmpty());

        // Only longitude provided
        CreatePracticeLocationRequest onlyLng = CreatePracticeLocationRequest.builder()
                .locationName("Branch Office")
                .addressLine1("Main Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .latitude(null)
                .longitude(new java.math.BigDecimal("72.877700"))
                .build();
        assertFalse(validator.validate(onlyLng).isEmpty());

        // Both provided
        CreatePracticeLocationRequest bothCoords = CreatePracticeLocationRequest.builder()
                .locationName("Branch Office")
                .addressLine1("Main Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .latitude(new java.math.BigDecimal("19.076000"))
                .longitude(new java.math.BigDecimal("72.877700"))
                .build();
        assertTrue(validator.validate(bothCoords).isEmpty());
    }

    @Test
    @DisplayName("Validation: geographic coordinates must remain within bounds (-90..90, -180..180)")
    void testLocationCoordinatesBoundsValidation() {
        CreatePracticeLocationRequest outOfBoundsLat = CreatePracticeLocationRequest.builder()
                .locationName("Branch Office")
                .addressLine1("Main Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .latitude(new java.math.BigDecimal("95.000000"))
                .longitude(new java.math.BigDecimal("72.877700"))
                .build();
        assertFalse(validator.validate(outOfBoundsLat).isEmpty());

        CreatePracticeLocationRequest outOfBoundsLng = CreatePracticeLocationRequest.builder()
                .locationName("Branch Office")
                .addressLine1("Main Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .latitude(new java.math.BigDecimal("19.076000"))
                .longitude(new java.math.BigDecimal("185.000000"))
                .build();
        assertFalse(validator.validate(outOfBoundsLng).isEmpty());
    }
}
