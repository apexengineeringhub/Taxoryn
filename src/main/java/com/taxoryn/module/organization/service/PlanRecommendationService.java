package com.taxoryn.module.organization.service;

import com.taxoryn.module.organization.dto.PlanRecommendationDto;
import com.taxoryn.module.organization.entity.PracticeProfileEntity;

public interface PlanRecommendationService {

    PlanRecommendationDto recommendPlan(PracticeProfileEntity profile);
}
