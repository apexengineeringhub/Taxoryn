package com.taxoryn.module.content.entity;

import com.taxoryn.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing permanent 301 alias/redirect records for published content slugs.
 */
@Entity
@Table(name = "content_slug_redirects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentSlugRedirectEntity extends BaseEntity {

    @Column(name = "old_slug", nullable = false, unique = true, length = 255)
    private String oldSlug;

    @Column(name = "new_slug", nullable = false, length = 255)
    private String newSlug;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", insertable = false, updatable = false)
    private ContentEntity content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    public void prePersistRedirect() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
