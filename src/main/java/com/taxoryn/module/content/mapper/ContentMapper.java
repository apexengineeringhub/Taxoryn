package com.taxoryn.module.content.mapper;

import com.taxoryn.module.content.dto.ContentResponse;
import com.taxoryn.module.content.dto.ContentSummaryResponse;
import com.taxoryn.module.content.dto.ContentTagDto;
import com.taxoryn.module.content.entity.ContentEntity;
import com.taxoryn.module.content.entity.ContentTagEntity;
import com.taxoryn.module.content.util.YouTubeUtils;
import com.taxoryn.module.marketplace.dto.PublicTaxServiceDto;
import com.taxoryn.module.marketplace.entity.TaxServiceEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ContentMapper {

    public ContentResponse toResponse(ContentEntity entity) {
        if (entity == null) {
            return null;
        }

        String effectiveThumbnail = resolveThumbnail(entity.getThumbnailUrl(), entity.getYoutubeVideoId());
        String embedUrl = YouTubeUtils.buildEmbedUrl(entity.getYoutubeVideoId());
        String watchUrl = YouTubeUtils.buildWatchUrl(entity.getYoutubeVideoId());
        String durationFormatted = YouTubeUtils.formatDuration(entity.getVideoDurationSeconds());
        List<PublicTaxServiceDto> activeTaxServices = resolveActiveTaxServices(entity);

        return ContentResponse.builder()
                .id(entity.getId())
                .contentType(entity.getContentType())
                .title(entity.getTitle())
                .slug(entity.getSlug())
                .summary(entity.getSummary())
                .body(entity.getBody())
                .thumbnailUrl(effectiveThumbnail)
                .youtubeVideoId(entity.getYoutubeVideoId())
                .youtubeEmbedUrl(embedUrl)
                .youtubeWatchUrl(watchUrl)
                .videoDurationSeconds(entity.getVideoDurationSeconds())
                .videoDurationFormatted(durationFormatted)
                .status(entity.getStatus())
                .categoryId(entity.getCategoryId())
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
                .categoryCode(entity.getCategory() != null ? entity.getCategory().getCode() : null)
                .taxServiceId(entity.getTaxServiceId())
                .taxServiceName(entity.getTaxService() != null ? entity.getTaxService().getName() : null)
                .taxServiceCode(entity.getTaxService() != null ? entity.getTaxService().getCode() : null)
                .taxServices(activeTaxServices)
                .marketplaceCtaEnabled(entity.isPublicReady() && !activeTaxServices.isEmpty())
                .scope(entity.getScope())
                .authorId(entity.getAuthorId())
                .authorName(entity.getAuthor() != null ? entity.getAuthor().getFirstName() + " " + (entity.getAuthor().getLastName() != null ? entity.getAuthor().getLastName() : "") : null)
                .authorEmail(entity.getAuthor() != null ? entity.getAuthor().getEmail() : null)
                .reviewerId(entity.getReviewerId())
                .reviewerName(entity.getReviewer() != null ? entity.getReviewer().getFirstName() + " " + (entity.getReviewer().getLastName() != null ? entity.getReviewer().getLastName() : "") : null)
                .reviewerEmail(entity.getReviewer() != null ? entity.getReviewer().getEmail() : null)
                .publishedAt(entity.getPublishedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .tags(toTagDtoList(entity.getTags()))
                .publicReady(entity.isPublicReady())
                .build();
    }

    public ContentSummaryResponse toSummaryResponse(ContentEntity entity) {
        if (entity == null) {
            return null;
        }

        String effectiveThumbnail = resolveThumbnail(entity.getThumbnailUrl(), entity.getYoutubeVideoId());
        String durationFormatted = YouTubeUtils.formatDuration(entity.getVideoDurationSeconds());
        List<PublicTaxServiceDto> activeTaxServices = resolveActiveTaxServices(entity);

        return ContentSummaryResponse.builder()
                .id(entity.getId())
                .contentType(entity.getContentType())
                .title(entity.getTitle())
                .slug(entity.getSlug())
                .summary(entity.getSummary())
                .thumbnailUrl(effectiveThumbnail)
                .youtubeVideoId(entity.getYoutubeVideoId())
                .videoDurationSeconds(entity.getVideoDurationSeconds())
                .videoDurationFormatted(durationFormatted)
                .status(entity.getStatus())
                .categoryId(entity.getCategoryId())
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
                .categoryCode(entity.getCategory() != null ? entity.getCategory().getCode() : null)
                .taxServiceId(entity.getTaxServiceId())
                .taxServiceName(entity.getTaxService() != null ? entity.getTaxService().getName() : null)
                .taxServiceCode(entity.getTaxService() != null ? entity.getTaxService().getCode() : null)
                .taxServices(activeTaxServices)
                .marketplaceCtaEnabled(entity.isPublicReady() && !activeTaxServices.isEmpty())
                .scope(entity.getScope())
                .authorId(entity.getAuthorId())
                .authorName(entity.getAuthor() != null ? entity.getAuthor().getFirstName() + " " + (entity.getAuthor().getLastName() != null ? entity.getAuthor().getLastName() : "") : null)
                .reviewerId(entity.getReviewerId())
                .reviewerName(entity.getReviewer() != null ? entity.getReviewer().getFirstName() + " " + (entity.getReviewer().getLastName() != null ? entity.getReviewer().getLastName() : "") : null)
                .publishedAt(entity.getPublishedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .tags(toTagDtoList(entity.getTags()))
                .publicReady(entity.isPublicReady())
                .build();
    }

    private List<PublicTaxServiceDto> resolveActiveTaxServices(ContentEntity entity) {
        Map<UUID, PublicTaxServiceDto> servicesMap = new LinkedHashMap<>();

        // 1. Check primary tax service
        if (entity.getTaxService() != null && Boolean.TRUE.equals(entity.getTaxService().getIsActive())) {
            TaxServiceEntity ts = entity.getTaxService();
            servicesMap.put(ts.getId(), toPublicTaxServiceDto(ts));
        }

        // 2. Check many-to-many tax services
        if (entity.getTaxServices() != null) {
            for (TaxServiceEntity ts : entity.getTaxServices()) {
                if (ts != null && Boolean.TRUE.equals(ts.getIsActive()) && !servicesMap.containsKey(ts.getId())) {
                    servicesMap.put(ts.getId(), toPublicTaxServiceDto(ts));
                }
            }
        }

        return new ArrayList<>(servicesMap.values());
    }

    private PublicTaxServiceDto toPublicTaxServiceDto(TaxServiceEntity ts) {
        return PublicTaxServiceDto.builder()
                .id(ts.getId())
                .code(ts.getCode())
                .name(ts.getName())
                .description(ts.getDescription())
                .category(ts.getCategory() != null ? ts.getCategory().getCode() : null)
                .categoryName(ts.getCategory() != null ? ts.getCategory().getName() : null)
                .sortOrder(ts.getSortOrder())
                .build();
    }

    private String resolveThumbnail(String customThumbnail, String youtubeVideoId) {
        if (StringUtils.hasText(customThumbnail)) {
            return customThumbnail.trim();
        }
        if (StringUtils.hasText(youtubeVideoId)) {
            return YouTubeUtils.buildThumbnailUrl(youtubeVideoId);
        }
        return null;
    }

    public ContentTagDto toTagDto(ContentTagEntity entity) {
        if (entity == null) return null;
        return ContentTagDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .build();
    }

    public List<ContentTagDto> toTagDtoList(Set<ContentTagEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(this::toTagDto)
                .collect(Collectors.toList());
    }
}

