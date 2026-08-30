package com.taxoryn.module.docrequest.dto;

import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document Request Detail & Progress")
public class DocumentRequestDto {

    private UUID id;
    private UUID organizationId;
    private UUID clientId;
    private String clientName;
    private String clientPan;
    private String requestNumber;
    private String purpose;
    private LocalDate dueDate;
    private String message;
    private RequestStatus status;
    private String financialYear;
    private String assessmentYear;
    private UUID requestedByUserId;
    private String requestedByName;
    private UUID taskId;
    private UUID complianceId;
    private Instant sentAt;
    private Instant completedAt;
    private Instant createdAt;
    private int totalItems;
    private int uploadedItems;
    private int acceptedItems;
    private int pendingItems;
    private int rejectedItems;
    private boolean isOverdue;

    @Builder.Default
    private List<DocumentRequestItemDto> items = new ArrayList<>();
}