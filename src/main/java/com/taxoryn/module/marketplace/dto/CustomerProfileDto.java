package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceCustomerProfileEntity.CustomerProfileStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceCustomerProfileEntity.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileDto {

    private UUID id;

    private UUID userId;

    private CustomerType customerType;

    private String firstName;

    private String lastName;

    private String displayName;

    private String email;

    private String phone;

    private String profilePhotoUrl;

    private String city;

    private String state;

    private String pincode;

    private String preferredLanguage;

    private String businessName;

    private CustomerProfileStatus status;

    private CustomerProfileCompletenessDto profileCompleteness;

    private Instant createdAt;

    private Instant updatedAt;
}
