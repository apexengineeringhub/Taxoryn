package com.taxoryn.module.content.mapper;

import com.taxoryn.module.content.dto.ContentResponse;
import com.taxoryn.module.content.dto.ContentSummaryResponse;
import com.taxoryn.module.content.dto.ContentTagDto;
import com.taxoryn.module.content.entity.ContentEntity;
import com.taxoryn.module.content.entity.ContentTagEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ContentMapper {

    public ContentResponse toResponse(ContentEntity entity) {
        if (entity == null) {
            return null;
        }

        return ContentResponse.builder()
                .id(entity.getId())
                .contentType(entity.getContentType())
                .title(entity.getTitle())
                .slug(entity.getSlug())
                .summary(entity.getSummary())
                .body(entity.getBody())
                .thumbnailUrl(entity.getThumbnailUrl())
                .status(entity.getStatus())
                .categoryId(entity.getCategoryId())
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
                .categoryCode(entity.getCategory() != null ? entity.getCategory().getCode() : null)
                .taxServiceId(entity.getTaxServiceId())
                .taxServiceName(entity.getTaxService() != null ? entity.getTaxService().getName() : null)
                .taxServiceCode(entity.getTaxService() != null ? entity.getTaxService().getCode() : null)
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

        return ContentSummaryResponse.builder()
                .id(entity.getId())
                .contentType(entity.getContentType())
                .title(entity.getTitle())
                .slug(entity.getSlug())
                .summary(entity.getSummary())
                .thumbnailUrl(entity.getThumbnailUrl())
                .status(entity.getStatus())
                .categoryId(entity.getCategoryId())
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
                .categoryCode(entity.getCategory() != null ? entity.getCategory().getCode() : null)
                .taxServiceId(entity.getTaxServiceId())
                .taxServiceName(entity.getTaxService() != null ? entity.getTaxService().getName() : null)
                .taxServiceCode(entity.getTaxService() != null ? entity.getTaxService().getCode() : null)
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
