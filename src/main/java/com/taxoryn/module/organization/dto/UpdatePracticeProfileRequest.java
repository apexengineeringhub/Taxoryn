package com.taxoryn.module.organization.dto;

import com.taxoryn.module.organization.entity.PracticeType;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePracticeProfileRequest {

    private PracticeType practiceType;

    @Min(value = 0, message = "Years in practice cannot be negative")
    private Integer yearsInPractice;

    @Min(value = 0, message = "Approximate client count cannot be negative")
    private Integer approximateClientCount;

    private List<String> servicesOffered;

    @Min(value = 1, message = "Practitioner count must be at least 1")
    private Integer practitionerCount;

    @Min(value = 1, message = "Employee count must be at least 1")
    private Integer employeeCount;

    @Min(value = 1, message = "Location count must be at least 1")
    private Integer locationCount;

    private List<String> primaryTaxServices;
}
