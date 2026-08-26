package com.taxoryn.module.content.dto;

import com.taxoryn.module.content.entity.ContentOwnershipScope;
import com.taxoryn.module.content.entity.ContentStatus;
import com.taxoryn.module.content.entity.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentResponse {

    private UUID id;
    private ContentType contentType;
    private String title;
    private String slug;
    private String summary;
    private String body;
    private String thumbnailUrl;
    private ContentStatus status;

    private UUID categoryId;
    private String categoryName;
    private String categoryCode;

    private UUID taxServiceId;
    private String taxServiceName;
    private String taxServiceCode;

    private ContentOwnershipScope scope;

    private UUID authorId;
    private String authorName;
    private String authorEmail;

    private UUID reviewerId;
    private String reviewerName;
    private String reviewerEmail;

    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    @Builder.Default
    private List<ContentTagDto> tags = new ArrayList<>();

    private boolean publicReady;
}
