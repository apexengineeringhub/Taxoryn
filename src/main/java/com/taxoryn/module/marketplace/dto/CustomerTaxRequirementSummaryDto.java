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
public class CustomerTaxRequirementSummaryDto {

    private UUID id;
    private UUID taxServiceId;
    private String taxServiceCode;
    private String taxServiceName;
    private String categoryName;
    private TaxRequirementStatus status;
    private String statusDisplayName;
    private CustomerTaxpayerType customerType;
    private String customerTypeDisplayName;
    private String financialYear;
    private String financialYearDisplay;
    private String city;
    private String state;
    private boolean editable;
    private boolean cancellable;
    private Instant createdAt;
    private Instant updatedAt;
}
