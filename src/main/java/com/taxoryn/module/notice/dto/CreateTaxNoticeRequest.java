package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaxNoticeRequest {

    @NotNull(message = "Client ID is required")
    private UUID clientId;

    @NotBlank(message = "Notice number is required")
    @Size(max = 100, message = "Notice number cannot exceed 100 characters")
    private String noticeNumber;

    @Size(max = 100, message = "DIN cannot exceed 100 characters")
    private String dinNumber;

    @NotNull(message = "Department is required")
    private NoticeDepartment department;

    @NotBlank(message = "Notice type is required")
    @Size(max = 100, message = "Notice type cannot exceed 100 characters")
    private String noticeType;

    @Size(max = 100, message = "Section cannot exceed 100 characters")
    private String section;

    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject cannot exceed 255 characters")
    private String subject;

    private String description;

    @Size(max = 20, message = "Assessment Year cannot exceed 20 characters")
    private String assessmentYear;

    @Size(max = 20, message = "Financial Year cannot exceed 20 characters")
    private String financialYear;

    @Size(max = 50, message = "Tax Period cannot exceed 50 characters")
    private String taxPeriod;

    private BigDecimal demandAmount;

    private LocalDate noticeDate;

    @NotNull(message = "Received date is required")
    private LocalDate receivedDate;

    @NotNull(message = "Response due date is required")
    private LocalDate responseDueDate;

    private LocalDate hearingDate;

    @Size(max = 20, message = "Hearing time cannot exceed 20 characters")
    private String hearingTime;

    private NoticePriority priority;

    private UUID assignedEmployeeId;
    private UUID reviewerEmployeeId;
    private UUID partnerEmployeeId;

    @Size(max = 255, message = "Issuing authority cannot exceed 255 characters")
    private String issuingAuthority;

    @Size(max = 150, message = "Issuing officer name cannot exceed 150 characters")
    private String issuingOfficerName;

    private String internalNotes;

    @Builder.Default
    private Boolean createIntakeTask = true;

    private UUID originalDocumentId;
}
