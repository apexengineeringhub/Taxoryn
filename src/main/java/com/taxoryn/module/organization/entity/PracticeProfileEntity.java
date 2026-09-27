package com.taxoryn.module.organization.entity;

import com.taxoryn.core.domain.AuditableEntity;
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

import java.util.UUID;

@Entity
@Table(name = "practice_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeProfileEntity extends AuditableEntity {

    @Column(name = "organization_id", nullable = false, unique = true)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "practice_type", nullable = false, length = 50)
    @Builder.Default
    private PracticeType practiceType = PracticeType.UNKNOWN;

    @Column(name = "years_in_practice")
    @Builder.Default
    private Integer yearsInPractice = 0;

    @Column(name = "approximate_client_count")
    @Builder.Default
    private Integer approximateClientCount = 0;

    @Column(name = "services_offered", columnDefinition = "TEXT")
    private String servicesOffered;

    @Column(name = "practitioner_count")
    @Builder.Default
    private Integer practitionerCount = 1;

    @Column(name = "employee_count")
    @Builder.Default
    private Integer employeeCount = 1;

    @Column(name = "location_count")
    @Builder.Default
    private Integer locationCount = 1;

    @Column(name = "primary_tax_services", columnDefinition = "TEXT")
    private String primaryTaxServices;

    @Column(name = "onboarding_completed", nullable = false)
    @Builder.Default
    private boolean onboardingCompleted = false;

    @Column(name = "recommended_plan", length = 50)
    private String recommendedPlan;

    @Column(name = "confirmed_plan", length = 50)
    private String confirmedPlan;
}
