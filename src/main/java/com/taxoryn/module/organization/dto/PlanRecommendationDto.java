package com.taxoryn.module.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanRecommendationDto {

    private String recommendedPlan;
    private String planName;
    private String justification;
    private List<String> matchingCriteria;
    private List<String> keyFeatures;
    private boolean multiLocationEnabled;
    private int maxLocations;
    private int maxUsers;
    private int maxClients;
}
