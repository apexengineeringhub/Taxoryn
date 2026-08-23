package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.CustomerTaxpayerType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaxRequirementRequest {

    private UUID taxServiceId;

    private String taxServiceCode;

    private CustomerTaxpayerType customerType;

    private String financialYear;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private String city;

    private String state;

    private String pincode;

    private Integer searchRadiusKm;
}
