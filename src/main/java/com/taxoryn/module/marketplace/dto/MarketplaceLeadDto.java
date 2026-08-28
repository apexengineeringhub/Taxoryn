package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.LeadStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.Urgency;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Inbound Marketplace Customer Inquiry / Lead DTO")
public class MarketplaceLeadDto {

    private UUID id;
    private UUID organizationId;
    private UUID marketplaceProfileId;
    private UUID serviceId;
    private String serviceTitle;
    private UUID taxServiceId;
    private String taxServiceName;
    private String sourceType;
    private UUID sourceContentId;
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private String city;
    private String pan;
    private String gstin;
    private String serviceCategory;
    private String requirementDescription;
    private String budgetRange;
    private Urgency urgency;
    private String referenceNumber;
    private String practiceName;
    private String practiceSlug;
    private com.taxoryn.module.marketplace.entity.EnquiryStatus enquiryStatus;
    private com.taxoryn.module.marketplace.entity.EnquiryRejectionReason rejectionReason;
    private String rejectionNote;
    private String cancellationReason;
    private Instant receivedAt;
    private Instant acceptedAt;
    private Instant rejectedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private UUID reviewId;
    private LeadStatus leadStatus;
    private UUID convertedClientId;
    private String convertedClientName;
    private UUID assignedEmployeeId;
    private String assignedEmployeeName;
    private String practitionerNotes;
    private Instant createdAt;
}
