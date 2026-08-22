package com.taxoryn.module.marketplace.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "marketplace_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceProfileEntity extends AuditableEntity {

    @Column(name = "organization_id", nullable = false, unique = true)
    private UUID organizationId;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "headline")
    private String headline;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "professional_type", nullable = false, length = 50)
    @Builder.Default
    private ProfessionalType professionalType = ProfessionalType.CHARTERED_ACCOUNTANT;

    @Column(name = "experience_years")
    @Builder.Default
    private Integer experienceYears = 5;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 20)
    private String pincode;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "banner_url", columnDefinition = "TEXT")
    private String bannerUrl;

    @Column(name = "specializations", columnDefinition = "TEXT")
    private String specializations;

    @Column(name = "languages_spoken")
    @Builder.Default
    private String languagesSpoken = "English, Hindi";

    @Column(name = "starting_fee", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal startingFee = new BigDecimal("999.00");

    @Column(name = "hourly_rate", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal hourlyRate = new BigDecimal("1500.00");

    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = new BigDecimal("4.90");

    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(name = "total_clients_served")
    @Builder.Default
    private Integer totalClientsServed = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "consultation_enabled", nullable = false)
    @Builder.Default
    private Boolean consultationEnabled = true;

    @Column(name = "consultation_fee", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal consultationFee = new BigDecimal("499.00");

    @Column(name = "consultation_duration_minutes")
    @Builder.Default
    private Integer consultationDurationMinutes = 30;

    public enum ProfessionalType {
        CHARTERED_ACCOUNTANT,
        COMPANY_SECRETARY,
        COST_ACCOUNTANT,
        TAX_ADVOCATE,
        TAX_CONSULTANT
    }

    public enum VerificationStatus {
        PENDING,
        VERIFIED,
        REJECTED
    }
}
