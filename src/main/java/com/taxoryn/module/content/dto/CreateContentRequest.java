package com.taxoryn.module.content.dto;

import com.taxoryn.module.content.entity.ContentOwnershipScope;
import com.taxoryn.module.content.entity.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateContentRequest {

    @NotNull(message = "Content type is required")
    private ContentType contentType;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @Size(max = 255, message = "Slug cannot exceed 255 characters")
    private String slug;

    private String summary;

    @NotBlank(message = "Content body is required")
    private String body;

    @Size(max = 500, message = "Thumbnail URL cannot exceed 500 characters")
    private String thumbnailUrl;

    private String youtubeUrl;

    private Integer videoDurationSeconds;

    private UUID categoryId;

    private UUID taxServiceId;

    @Builder.Default
    private Set<UUID> taxServiceIds = new HashSet<>();

    @Builder.Default
    private ContentOwnershipScope scope = ContentOwnershipScope.PLATFORM;

    @Builder.Default
    private Set<String> tags = new HashSet<>();
}
