package com.taxoryn.module.capability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing persona-tailored dashboard layout, focus metrics, and quick actions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardProfileDto {

    private String profileKey;
    private String title;
    private String description;
    private String defaultRoute;
    private List<String> primaryMetrics;
    private List<String> quickActions;
    private List<String> recommendedWidgets;
}
