package com.taxoryn.module.organization.dto;

import com.taxoryn.module.organization.entity.PracticeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeProfileDto {

    private UUID id;
    private UUID organizationId;
    private String organizationName;
    private PracticeType practiceType;
    private Integer yearsInPractice;
    private Integer approximateClientCount;
    private List<String> servicesOffered;
    private Integer practitionerCount;
    private Integer employeeCount;
    private Integer locationCount;
    private List<String> primaryTaxServices;
    private boolean onboardingCompleted;
    private String recommendedPlan;
    private String confirmedPlan;
    private Instant createdAt;
    private Instant updatedAt;
}
