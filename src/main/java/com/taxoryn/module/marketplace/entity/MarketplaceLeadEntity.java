package com.taxoryn.module.marketplace.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "marketplace_leads")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceLeadEntity extends AuditableEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "marketplace_profile_id", nullable = false)
    private UUID marketplaceProfileId;

    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "client_email", nullable = false)
    private String clientEmail;

    @Column(name = "client_phone", nullable = false, length = 20)
    private String clientPhone;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "pan", length = 10)
    private String pan;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "service_category", length = 100)
    private String serviceCategory;

    @Column(name = "requirement_description", columnDefinition = "TEXT")
    private String requirementDescription;

    @Column(name = "budget_range", length = 50)
    private String budgetRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", length = 50)
    @Builder.Default
    private Urgency urgency = Urgency.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_status", nullable = false, length = 50)
    @Builder.Default
    private LeadStatus leadStatus = LeadStatus.NEW;

    @Column(name = "converted_client_id")
    private UUID convertedClientId;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "practitioner_notes", columnDefinition = "TEXT")
    private String practitionerNotes;

    public enum LeadStatus {
        NEW,
        CONTACTED,
        PROPOSAL_SENT,
        ACCEPTED,
        CONVERTED,
        CLOSED_LOST,
        ARCHIVED
    }

    public enum Urgency {
        LOW,
        STANDARD,
        URGENT
    }
}
