package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceCustomerProfileEntity.CustomerType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerProfileRequest {

    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Size(max = 255, message = "Display name cannot exceed 255 characters")
    private String displayName;

    @Pattern(regexp = "^(\\+91)?[6-9]\\d{9}$", message = "Invalid Indian mobile number format")
    private String phone;

    @Size(max = 500, message = "Profile photo URL cannot exceed 500 characters")
    private String profilePhotoUrl;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @Size(max = 20, message = "Pincode cannot exceed 20 characters")
    private String pincode;

    @Size(max = 50, message = "Preferred language cannot exceed 50 characters")
    private String preferredLanguage;

    private CustomerType customerType;

    @Size(max = 255, message = "Business name cannot exceed 255 characters")
    private String businessName;
}
