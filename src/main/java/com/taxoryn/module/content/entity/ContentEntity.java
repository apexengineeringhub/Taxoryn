package com.taxoryn.module.content.entity;

import com.taxoryn.core.domain.AuditableEntity;
import com.taxoryn.module.marketplace.entity.TaxServiceCategoryEntity;
import com.taxoryn.module.marketplace.entity.TaxServiceEntity;
import com.taxoryn.module.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "contents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentEntity extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 50)
    private ContentType contentType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "youtube_video_id", length = 64)
    private String youtubeVideoId;

    @Column(name = "video_duration_seconds")
    private Integer videoDurationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ContentStatus status = ContentStatus.DRAFT;

    @Column(name = "category_id")
    private UUID categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private TaxServiceCategoryEntity category;

    @Column(name = "tax_service_id")
    private UUID taxServiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_service_id", insertable = false, updatable = false)
    private TaxServiceEntity taxService;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 50)
    @Builder.Default
    private ContentOwnershipScope scope = ContentOwnershipScope.PLATFORM;

    @Column(name = "author_id")
    private UUID authorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", insertable = false, updatable = false)
    private UserEntity author;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", insertable = false, updatable = false)
    private UserEntity reviewer;

    @Column(name = "published_at")
    private Instant publishedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "content_tag_mappings",
            joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<ContentTagEntity> tags = new HashSet<>();

    /**
     * Domain rule: only PUBLISHED content is public-ready.
     */
    public boolean isPublicReady() {
        return this.status == ContentStatus.PUBLISHED;
    }
}
