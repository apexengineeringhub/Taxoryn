package com.taxoryn.module.content.dto;

import com.taxoryn.module.content.entity.ContentOwnershipScope;
import com.taxoryn.module.content.entity.ContentType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateContentRequest {

    private ContentType contentType;

    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @Size(max = 255, message = "Slug cannot exceed 255 characters")
    private String slug;

    private String summary;

    private String body;

    @Size(max = 500, message = "Thumbnail URL cannot exceed 500 characters")
    private String thumbnailUrl;

    @Size(max = 500, message = "Featured Image URL cannot exceed 500 characters")
    private String featuredImageUrl;

    @Size(max = 255, message = "Alt text cannot exceed 255 characters")
    private String altText;

    @Size(max = 255, message = "SEO Title cannot exceed 255 characters")
    private String seoTitle;

    @Size(max = 500, message = "Meta Description cannot exceed 500 characters")
    private String metaDescription;

    @Size(max = 500, message = "Canonical URL cannot exceed 500 characters")
    private String canonicalUrl;

    private String youtubeUrl;

    private Integer videoDurationSeconds;

    private UUID categoryId;

    private UUID taxServiceId;

    private Set<UUID> taxServiceIds;

    private ContentOwnershipScope scope;

    private Set<String> tags;
}
