package com.taxoryn.module.content.entity;

import com.taxoryn.core.domain.BaseEntity;
import com.taxoryn.module.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.util.UUID;

/**
 * Media library asset metadata entity for digital assets.
 */
@Entity
@Table(name = "content_media_assets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetEntity extends BaseEntity {

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "public_url", nullable = false, length = 500)
    private String publicUrl;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "uploaded_by_id")
    private UUID uploadedById;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", insertable = false, updatable = false)
    private UserEntity uploadedBy;

    @Column(name = "uploaded_by_name", length = 255)
    private String uploadedByName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    public void prePersistMedia() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
