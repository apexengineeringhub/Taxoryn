package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.CustomerTaxpayerType;
import com.taxoryn.module.marketplace.entity.EnquiryRejectionReason;
import com.taxoryn.module.marketplace.entity.EnquiryStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.Urgency;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnquiryDetailDto {
    private UUID id;
    private String referenceNumber;
    private UUID organizationId;
    private String practiceName;
    private String practiceSlug;
    private String practiceCity;
    private UUID marketplaceProfileId;
    private UUID customerId;
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private String city;

    // Service Context
    private UUID taxServiceId;
    private String taxServiceName;
    private String taxServiceCode;
    private String serviceCategory;
    private String financialYear;
    private CustomerTaxpayerType customerType;

    // Requirement & Privacy Safe Messages
    private String requirementDescription;
    private String earlyEnquiryMessage;
    private String budgetRange;
    private Urgency urgency;
    private String sourceType;

    // Lifecycle Status & Reasons
    private EnquiryStatus enquiryStatus;
    private EnquiryRejectionReason rejectionReason;
    private String rejectionNote;
    private String cancellationReason;
    private String practitionerNotes;

    // Employee Assignment
    private UUID assignedEmployeeId;
    private String assignedEmployeeName;

    // Timestamps
    private Instant createdAt;
    private Instant receivedAt;
    private Instant acceptedAt;
    private Instant rejectedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    // Visual Timeline
    private List<EnquiryTimelineItemDto> timeline;

    // Capabilities / Actions
    private boolean canCancel;
    private boolean canReview;
    private UUID reviewId;
}
