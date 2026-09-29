package com.taxoryn.module.worktemplate.dto;

import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.worktemplate.model.RecurrenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementWorkTemplateDto {

    private UUID id;
    private UUID organizationId;
    private UUID engagementId;
    private UUID templateId;
    private String templateCode;
    private String templateName;
    private String templateDescription;
    private ServiceCategory category;
    private boolean active;
    private RecurrenceType recurrenceType;
    private int recurrenceInterval;
    private Integer dayOfMonth;
    private Integer monthOfYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate lastGeneratedPeriodStart;
    private LocalDate lastGeneratedPeriodEnd;
    private int taskCount;
    private Instant createdAt;
    private Instant updatedAt;
}
