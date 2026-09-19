package com.taxoryn.module.marketing.entity;

import com.taxoryn.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "early_access_requests")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarlyAccessRequestEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "practice_name", nullable = false, length = 200)
    private String practiceName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "practice_profile", length = 100)
    private String practiceProfile;

    @Column(name = "primary_area", length = 100)
    private String primaryArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private EarlyAccessStatus status = EarlyAccessStatus.NEW;

    @Column(name = "source", length = 100)
    @Builder.Default
    private String source = "MARKETING_WEBSITE";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
