package com.taxoryn.module.capability.dto;

import com.taxoryn.module.capability.model.ProductCapability;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an actionable onboarding checklist step tailored to the organization persona.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingStepDto {

    private String stepKey;
    private String title;
    private String description;
    private String targetRoute;
    private int sortOrder;
    private boolean mandatory;
    private ProductCapability targetCapability;
}
