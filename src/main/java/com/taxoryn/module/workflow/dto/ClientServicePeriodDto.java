package com.taxoryn.module.workflow.dto;

import com.taxoryn.module.workflow.model.ServicePeriodStatus;
import com.taxoryn.module.workflow.model.ServicePeriodType;
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
public class ClientServicePeriodDto {
    private UUID id;
    private UUID organizationId;
    private UUID clientId;
    private String clientName;
    private UUID clientServiceId;
    private String serviceType;
    private String serviceName;
    private ServicePeriodType periodType;
    private String periodLabel;
    private String financialYear;
    private String assessmentYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate dueDate;
    private ServicePeriodStatus status;
    private boolean hasActiveWorkflow;
    private UUID activeWorkflowId;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
