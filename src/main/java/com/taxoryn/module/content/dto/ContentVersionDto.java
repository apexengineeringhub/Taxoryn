package com.taxoryn.module.content.dto;

import com.taxoryn.module.content.entity.ContentStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentVersionDto {

    private UUID id;
    private UUID contentId;
    private Integer versionNumber;
    private String title;
    private String summary;
    private String body;
    private String thumbnailUrl;
    private String featuredImageUrl;
    private String altText;
    private ContentStatus status;
    private String changeSummary;
    private String createdBy;
    private Instant createdAt;
}
