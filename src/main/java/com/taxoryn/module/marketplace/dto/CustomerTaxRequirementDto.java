package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.CustomerTaxpayerType;
import com.taxoryn.module.marketplace.entity.TaxRequirementStatus;
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
public class CustomerTaxRequirementDto {

    private UUID id;
    private UUID customerId;
    private PublicTaxServiceDto service;
    private TaxRequirementStatus status;
    private String statusDisplayName;
    private CustomerTaxpayerType customerType;
    private String customerTypeDisplayName;
    private String financialYear;
    private String financialYearDisplay;
    private String description;
    private String city;
    private String state;
    private String pincode;
    private Integer searchRadiusKm;
    private String sourceType;
    private UUID sourceContentId;
    private boolean editable;
    private boolean cancellable;
    private Instant createdAt;
    private Instant updatedAt;
}
