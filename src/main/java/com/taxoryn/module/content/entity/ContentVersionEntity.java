package com.taxoryn.module.content.entity;

import com.taxoryn.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable historical version snapshot for platform educational content.
 */
@Entity
@Table(name = "content_versions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentVersionEntity extends BaseEntity {

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", insertable = false, updatable = false)
    private ContentEntity content;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "featured_image_url", length = 500)
    private String featuredImageUrl;

    @Column(name = "alt_text")
    private String altText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ContentStatus status;

    @Column(name = "change_summary", length = 500)
    private String changeSummary;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "created_by")
    private String createdBy;

    @PrePersist
    public void prePersistVersion() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
