package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.entity.ComplianceWorkStatus;
import com.taxoryn.module.compliance.entity.ComplianceWorkType;
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
public class ComplianceWorkItemDto {

    private UUID id;
    private UUID organizationId;
    private UUID clientId;
    private String clientName;
    private UUID clientServiceId;
    private String serviceName;
    private String serviceType;
    private ComplianceWorkType workType;
    private String title;
    private String description;
    private String financialYear;
    private String assessmentYear;
    private String compliancePeriod;
    private ComplianceWorkStatus status;
    private LocalDate statutoryDueDate;
    private LocalDate internalTargetDate;
    private UUID assignedEmployeeId;
    private String assignedEmployeeName;
    private String assignedEmployeeEmail;
    private UUID reviewerEmployeeId;
    private String reviewerEmployeeName;
    private String reviewerEmployeeEmail;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
    private boolean overdue;
}
