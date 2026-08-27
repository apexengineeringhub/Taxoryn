package com.taxoryn.module.marketplace.entity;

import com.taxoryn.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_profile_slug_redirects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MarketplaceProfileSlugRedirectEntity extends BaseEntity {

    @Column(name = "old_slug", nullable = false, unique = true, length = 255)
    private String oldSlug;

    @Column(name = "new_slug", nullable = false, length = 255)
    private String newSlug;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
