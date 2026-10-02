package com.taxoryn.module.lead.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "practice_leads")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PracticeLeadEntity extends TenantAuditableEntity {
    @Enumerated(EnumType.STRING) @Column(name = "lead_type", nullable = false, length = 24)
    @Builder.Default private LeadType leadType = LeadType.INDIVIDUAL;
    @Column(nullable = false) private String name;
    @Column(name = "business_name") private String businessName;
    @Column private String email;
    @Column(length = 50) private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32)
    @Builder.Default private LeadSource source = LeadSource.OTHER;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32)
    @Builder.Default private LeadStatus status = LeadStatus.NEW;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    @Builder.Default private LeadPriority priority = LeadPriority.MEDIUM;
    @Column(name = "interested_service_code", length = 100) private String interestedServiceCode;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "assigned_employee_id") private UUID assignedEmployeeId;
    @Column(name = "next_follow_up_at") private Instant nextFollowUpAt;
    @Column(name = "converted_client_id") private UUID convertedClientId;
    @Column(name = "converted_at") private Instant convertedAt;
    @Column(name = "converted_by") private UUID convertedBy;
    @Column(name = "lost_reason", length = 500) private String lostReason;

    public enum LeadType { INDIVIDUAL, BUSINESS }
    public enum LeadSource { WEBSITE, PHONE, EMAIL, WALK_IN, REFERRAL, MARKETPLACE, SOCIAL_MEDIA, CAMPAIGN, OTHER }
    public enum LeadStatus { NEW, CONTACTED, QUALIFIED, PROPOSAL_SENT, FOLLOW_UP, CONVERTED, LOST }
    public enum LeadPriority { CRITICAL, URGENT, HIGH, MEDIUM, LOW }
}
