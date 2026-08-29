package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstReturnType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "GST Return Filing Record Details")
public class GstReturnFilingDto {

    @Schema(description = "Filing ID")
    private UUID id;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "GST Profile ID")
    private UUID gstProfileId;

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Client display name", example = "ABC Traders")
    private String clientName;

    @Schema(description = "GSTIN", example = "27AAACZ1234D1Z8")
    private String gstin;

    @Schema(description = "Return Type (e.g. GSTR1, GSTR3B, GSTR9, CMP08)", example = "GSTR1")
    private GstReturnType returnType;

    @Schema(description = "Return period (e.g. 2026-08)", example = "2026-08")
    private String returnPeriod;

    @Schema(description = "Financial Year (e.g. 2026-27)", example = "2026-27")
    private String financialYear;

    @Schema(description = "Statutory due date", example = "2026-09-11")
    private LocalDate dueDate;

    @Schema(description = "Current filing lifecycle status", example = "PENDING")
    private GstFilingStatus filingStatus;

    @Schema(description = "Actual date of filing", example = "2026-09-10")
    private LocalDate filingDate;

    @Schema(description = "GST Portal Acknowledgement Reference Number (ARN)", example = "AA2708260012345")
    private String acknowledgementNumber;

    @Schema(description = "Total taxable turnover / supplies value", example = "1500000.00")
    private BigDecimal totalTaxableValue;

    @Schema(description = "Total output tax liability", example = "270000.00")
    private BigDecimal totalTaxLiability;

    @Schema(description = "Total Input Tax Credit (ITC) claimed", example = "125000.00")
    private BigDecimal totalItcClaimed;

    @Schema(description = "Tax paid through electronic cash ledger", example = "145000.00")
    private BigDecimal taxPaidCash;

    @Schema(description = "Tax paid through electronic credit ledger (ITC)", example = "125000.00")
    private BigDecimal taxPaidItc;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Assigned practitioner full name", example = "Rahul Sharma")
    private String assignedEmployeeName;

    @Schema(description = "Practitioner notes or remarks")
    private String notes;

    @Schema(description = "Linked compliance obligation ID")
    private UUID complianceId;

    @Schema(description = "Linked compliance obligation title", example = "GSTR-3B Monthly Return - July 2026")
    private String complianceTitle;

    @Schema(description = "Linked task ID in Task Management module")
    private UUID taskId;

    @Schema(description = "Linked task title")
    private String taskTitle;

    @Schema(description = "Linked task workflow status (TODO, IN_PROGRESS, UNDER_REVIEW, BLOCKED, COMPLETED)")
    private String taskStatus;

    @Schema(description = "Linked document request ID")
    private UUID documentRequestId;

    @Schema(description = "Linked document request reference number", example = "REQ-2026-000123")
    private String documentRequestNumber;

    @Schema(description = "Linked document request status", example = "SENT")
    private String documentRequestStatus;

    @Schema(description = "Total document checklist items requested")
    private Integer documentRequestItemsCount;

    @Schema(description = "Total document checklist items received/uploaded")
    private Integer documentRequestReceivedCount;

    @Schema(description = "Count of attached documents in Document Vault")
    private Integer documentsCount;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
