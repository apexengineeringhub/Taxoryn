package com.taxoryn.module.content.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentDashboardStatsDto {

    private long totalContent;
    private long publishedCount;
    private long draftCount;
    private long inReviewCount;
    private long scheduledCount;
    private long archivedCount;
    private long rejectedCount;

    private List<ContentAttentionItemDto> needsAttention;
    private List<ContentActivityItemDto> recentActivity;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentAttentionItemDto {
        private String id;
        private String title;
        private String contentType;
        private String status;
        private String message;
        private Instant updatedAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentActivityItemDto {
        private String id;
        private String action;
        private String contentTitle;
        private String contentType;
        private String userName;
        private Instant timestamp;
    }
}
