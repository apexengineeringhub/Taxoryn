package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.entity.ComplianceWorkStatus;
import com.taxoryn.module.compliance.entity.ComplianceWorkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceWorkFilterRequest {

    private UUID clientId;
    private UUID clientServiceId;
    private ComplianceWorkType workType;
    private ComplianceWorkStatus status;
    private UUID assignedEmployeeId;
    private UUID reviewerEmployeeId;
    private String financialYear;
    private String assessmentYear;
    private String compliancePeriod;
    private String search;
    private Boolean overdue;
    private Boolean myWorkOnly;
    private Boolean pendingReviewOnly;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueTo;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    @Builder.Default
    private String sortBy = "statutoryDueDate";

    @Builder.Default
    private String sortDirection = "ASC";
}
