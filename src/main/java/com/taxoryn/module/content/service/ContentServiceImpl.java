package com.taxoryn.module.content.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.content.dto.*;
import com.taxoryn.module.content.entity.*;
import com.taxoryn.module.content.mapper.ContentMapper;
import com.taxoryn.module.content.repository.ContentRepository;
import com.taxoryn.module.content.repository.ContentSlugRedirectRepository;
import com.taxoryn.module.content.repository.ContentTagRepository;
import com.taxoryn.module.content.repository.ContentVersionRepository;
import com.taxoryn.module.content.util.YouTubeUtils;
import com.taxoryn.module.marketplace.entity.TaxServiceCategoryEntity;
import com.taxoryn.module.marketplace.entity.TaxServiceEntity;
import com.taxoryn.module.marketplace.repository.TaxServiceCategoryRepository;
import com.taxoryn.module.marketplace.repository.TaxServiceRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private final ContentRepository contentRepository;
    private final ContentVersionRepository contentVersionRepository;
    private final ContentSlugRedirectRepository contentSlugRedirectRepository;
    private final ContentTagRepository tagRepository;
    private final TaxServiceCategoryRepository categoryRepository;
    private final TaxServiceRepository taxServiceRepository;
    private final ContentMapper mapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public ContentResponse createContent(CreateContentRequest request) {
        if (request.getContentType() == null) {
            throw new BusinessValidationException("Content type is required");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessValidationException("Content title is required");
        }
        if (!StringUtils.hasText(request.getBody())) {
            throw new BusinessValidationException("Content body is required");
        }

        String slug = normalizeSlug(request.getSlug(), request.getTitle());
        if (contentRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Content with slug '" + slug + "' already exists");
        }

        TaxServiceCategoryEntity category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("TaxServiceCategory", "id", request.getCategoryId()));
        }

        TaxServiceEntity primaryTaxService = null;
        Set<TaxServiceEntity> resolvedTaxServices = new LinkedHashSet<>();

        if (request.getTaxServiceId() != null) {
            primaryTaxService = taxServiceRepository.findById(request.getTaxServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("TaxService", "id", request.getTaxServiceId()));
            if (!Boolean.TRUE.equals(primaryTaxService.getIsActive())) {
                throw new BusinessValidationException("Cannot attach inactive Tax Service: " + primaryTaxService.getName());
            }
            resolvedTaxServices.add(primaryTaxService);
        }

        if (request.getTaxServiceIds() != null && !request.getTaxServiceIds().isEmpty()) {
            for (UUID tsId : request.getTaxServiceIds()) {
                if (tsId != null) {
                    TaxServiceEntity svc = taxServiceRepository.findById(tsId)
                            .orElseThrow(() -> new ResourceNotFoundException("TaxService", "id", tsId));
                    if (!Boolean.TRUE.equals(svc.getIsActive())) {
                        throw new BusinessValidationException("Cannot attach inactive Tax Service: " + svc.getName());
                    }
                    resolvedTaxServices.add(svc);
                    if (primaryTaxService == null) {
                        primaryTaxService = svc;
                    }
                }
            }
        }

        String youtubeVideoId = null;
        if (StringUtils.hasText(request.getYoutubeUrl())) {
            youtubeVideoId = YouTubeUtils.extractVideoId(request.getYoutubeUrl());
            if (youtubeVideoId == null) {
                throw new BusinessValidationException("Please enter a valid YouTube video link.");
            }
        } else if (request.getContentType() == ContentType.VIDEO) {
            throw new BusinessValidationException("Please enter a valid YouTube video link.");
        }

        String thumbnailUrl = request.getThumbnailUrl();
        if (!StringUtils.hasText(thumbnailUrl) && youtubeVideoId != null) {
            thumbnailUrl = YouTubeUtils.buildThumbnailUrl(youtubeVideoId);
        }

        UUID currentUserId = SecurityUtils.getCurrentUser().map(u -> u.getUserId()).orElse(null);
        Set<ContentTagEntity> resolvedTags = resolveOrCreateTags(request.getTags());

        ContentEntity entity = ContentEntity.builder()
                .contentType(request.getContentType())
                .title(request.getTitle().trim())
                .slug(slug)
                .summary(StringUtils.hasText(request.getSummary()) ? request.getSummary().trim() : null)
                .body(request.getBody().trim())
                .thumbnailUrl(thumbnailUrl)
                .featuredImageUrl(request.getFeaturedImageUrl())
                .altText(request.getAltText())
                .seoTitle(StringUtils.hasText(request.getSeoTitle()) ? request.getSeoTitle().trim() : null)
                .metaDescription(StringUtils.hasText(request.getMetaDescription()) ? request.getMetaDescription().trim() : null)
                .canonicalUrl(StringUtils.hasText(request.getCanonicalUrl()) ? request.getCanonicalUrl().trim() : null)
                .youtubeVideoId(youtubeVideoId)
                .videoDurationSeconds(request.getVideoDurationSeconds())
                .status(ContentStatus.DRAFT)
                .versionNumber(1)
                .categoryId(category != null ? category.getId() : null)
                .category(category)
                .taxServiceId(primaryTaxService != null ? primaryTaxService.getId() : null)
                .taxService(primaryTaxService)
                .taxServices(resolvedTaxServices)
                .scope(request.getScope() != null ? request.getScope() : ContentOwnershipScope.PLATFORM)
                .authorId(currentUserId)
                .tags(resolvedTags)
                .build();

        ContentEntity saved = contentRepository.save(entity);
        log.info("Created platform content: id={}, title='{}', type={}, slug='{}'", saved.getId(), saved.getTitle(), saved.getContentType(), saved.getSlug());

        auditService.logEvent("CONTENT_CREATED", "CONTENT", saved.getId().toString(), null, "Type: " + saved.getContentType() + ", Title: " + saved.getTitle());

        return getContentById(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ContentResponse getContentById(UUID id) {
        ContentEntity entity = contentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", id));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ContentResponse getContentBySlug(String slug) {
        ContentEntity entity = contentRepository.findBySlugWithDetails(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "slug", slug));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public ContentResponse updateContent(UUID id, UpdateContentRequest request) {
        ContentEntity entity = contentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", id));

        // If published content is being edited, preserve a version snapshot
        if (entity.getStatus() == ContentStatus.PUBLISHED) {
            saveVersionSnapshot(entity, "Snapshot before modification of published content");
            entity.setVersionNumber((entity.getVersionNumber() != null ? entity.getVersionNumber() : 1) + 1);
        }

        if (request.getContentType() != null) {
            entity.setContentType(request.getContentType());
        }

        if (StringUtils.hasText(request.getTitle())) {
            entity.setTitle(request.getTitle().trim());
        }

        if (StringUtils.hasText(request.getSlug())) {
            String newSlug = normalizeSlug(request.getSlug(), entity.getTitle());
            if (!newSlug.equals(entity.getSlug())) {
                if (contentRepository.existsBySlugAndIdNot(newSlug, id)) {
                    throw new DuplicateResourceException("Content with slug '" + newSlug + "' already exists");
                }
                String oldSlug = entity.getSlug();
                if (entity.getStatus() == ContentStatus.PUBLISHED) {
                    // Record old slug -> new slug permanent redirect mapping
                    ContentSlugRedirectEntity redirect = ContentSlugRedirectEntity.builder()
                            .oldSlug(oldSlug)
                            .newSlug(newSlug)
                            .contentId(entity.getId())
                            .build();
                    contentSlugRedirectRepository.save(redirect);
                    // Prevent redirect chains by flattening any prior pointers to oldSlug directly to newSlug
                    contentSlugRedirectRepository.flattenRedirectChains(oldSlug, newSlug);
                    log.info("Registered slug redirect from '{}' -> '{}' for published content id={}", oldSlug, newSlug, entity.getId());
                }
                entity.setSlug(newSlug);
            }
        }

        if (request.getSeoTitle() != null) {
            entity.setSeoTitle(StringUtils.hasText(request.getSeoTitle()) ? request.getSeoTitle().trim() : null);
        }

        if (request.getMetaDescription() != null) {
            entity.setMetaDescription(StringUtils.hasText(request.getMetaDescription()) ? request.getMetaDescription().trim() : null);
        }

        if (request.getCanonicalUrl() != null) {
            entity.setCanonicalUrl(StringUtils.hasText(request.getCanonicalUrl()) ? request.getCanonicalUrl().trim() : null);
        }

        if (request.getSummary() != null) {
            entity.setSummary(StringUtils.hasText(request.getSummary()) ? request.getSummary().trim() : null);
        }

        if (StringUtils.hasText(request.getBody())) {
            entity.setBody(request.getBody().trim());
        }

        if (request.getThumbnailUrl() != null) {
            entity.setThumbnailUrl(StringUtils.hasText(request.getThumbnailUrl()) ? request.getThumbnailUrl().trim() : null);
        }

        if (request.getFeaturedImageUrl() != null) {
            entity.setFeaturedImageUrl(StringUtils.hasText(request.getFeaturedImageUrl()) ? request.getFeaturedImageUrl().trim() : null);
        }

        if (request.getAltText() != null) {
            entity.setAltText(StringUtils.hasText(request.getAltText()) ? request.getAltText().trim() : null);
        }

        if (request.getYoutubeUrl() != null) {
            if (StringUtils.hasText(request.getYoutubeUrl())) {
                String vid = YouTubeUtils.extractVideoId(request.getYoutubeUrl());
                if (vid == null) {
                    throw new BusinessValidationException("Please enter a valid YouTube video link.");
                }
                entity.setYoutubeVideoId(vid);
            } else {
                entity.setYoutubeVideoId(null);
            }
        }

        if (request.getVideoDurationSeconds() != null) {
            entity.setVideoDurationSeconds(request.getVideoDurationSeconds());
        }

        if (request.getCategoryId() != null) {
            TaxServiceCategoryEntity cat = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("TaxServiceCategory", "id", request.getCategoryId()));
            entity.setCategoryId(request.getCategoryId());
            entity.setCategory(cat);
        }

        if (request.getTaxServiceId() != null || request.getTaxServiceIds() != null) {
            Set<TaxServiceEntity> updatedTaxServices = new LinkedHashSet<>();
            TaxServiceEntity newPrimary = null;

            if (request.getTaxServiceId() != null) {
                newPrimary = taxServiceRepository.findById(request.getTaxServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("TaxService", "id", request.getTaxServiceId()));
                if (!Boolean.TRUE.equals(newPrimary.getIsActive())) {
                    throw new BusinessValidationException("Cannot attach inactive Tax Service: " + newPrimary.getName());
                }
                updatedTaxServices.add(newPrimary);
            }

            if (request.getTaxServiceIds() != null) {
                for (UUID tsId : request.getTaxServiceIds()) {
                    if (tsId != null) {
                        TaxServiceEntity svc = taxServiceRepository.findById(tsId)
                                .orElseThrow(() -> new ResourceNotFoundException("TaxService", "id", tsId));
                        if (!Boolean.TRUE.equals(svc.getIsActive())) {
                            throw new BusinessValidationException("Cannot attach inactive Tax Service: " + svc.getName());
                        }
                        updatedTaxServices.add(svc);
                        if (newPrimary == null) {
                            newPrimary = svc;
                        }
                    }
                }
            }

            entity.setTaxServiceId(newPrimary != null ? newPrimary.getId() : null);
            entity.setTaxService(newPrimary);
            entity.setTaxServices(updatedTaxServices);
        }

        if (request.getScope() != null) {
            entity.setScope(request.getScope());
        }

        if (request.getTags() != null) {
            entity.setTags(resolveOrCreateTags(request.getTags()));
        }

        ContentEntity saved = contentRepository.save(entity);
        log.info("Updated platform content: id={}, title='{}', type={}", saved.getId(), saved.getTitle(), saved.getContentType());

        auditService.logEvent("CONTENT_UPDATED", "CONTENT", saved.getId().toString(), null, saved.getTitle());

        return getContentById(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ContentSummaryResponse> listContent(ContentFilterRequest filterRequest) {
        Pageable pageable = filterRequest.toPageable();
        Specification<ContentEntity> spec = createSpecification(filterRequest);

        Page<ContentEntity> page = contentRepository.findAll(spec, pageable);
        return PagedResponse.of(page, mapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public ContentResponse submitForReview(UUID id) {
        ContentEntity entity = contentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", id));

        validateTransition(entity, ContentStatus.SUBMITTED);

        entity.setStatus(ContentStatus.SUBMITTED);
        entity.setRejectionReason(null);
        ContentEntity saved = contentRepository.save(entity);
        log.info("Submitted content for review: id={}, title='{}'", saved.getId(), saved.getTitle());

        auditService.logEvent("CONTENT_SUBMITTED", "CONTENT", saved.getId().toString(), null, "Status: SUBMITTED");

        return getContentById(saved.getId());
    }

    @Override
    @Transactional
    public ContentResponse startReview(UUID id) {
        ContentEntity entity = contentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", id));

        validateTransition(entity, ContentStatus.IN_REVIEW);

        UUID currentUserId = SecurityUtils.getCurrentUser().map(u -> u.getUserId()).orElse(null);
        entity.setStatus(ContentStatus.IN_REVIEW);
        entity.setReviewerId(currentUserId);

        ContentEntity saved = contentRepository.save(entity);
        log.info("Started review for content: id={}, reviewer={}", saved.getId(), currentUserId);

        auditService.logEvent("CONTENT_REVIEW_STARTED", "CONTENT", saved.getId().toString(), null, "Status: IN_REVIEW");

        return getContentById(saved.getId());
    }

    @Override
    @Transactional
    public ContentResponse approveContent(UUID id) {
        ContentEntity entity = contentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", id));

        validateTransition(entity, ContentStatus.APPROVED);

        UUID currentUserId = SecurityUtils.getCurrentUser().map(u -> u.getUserId()).orElse(null);
        entity.setStatus(ContentStatus.APPROVED);
        entity.setReviewerId(currentUserId);
        entity.setRejectionReason(null);

        ContentEntity saved = contentRepository.save(entity);
        log.info("Approved platform content: id={}, title='{}', reviewer={}", saved.getId(), saved.getTitle(), currentUserId);

        auditService.logEvent("CONTENT_APPROVED", "CONTENT", saved.getId().toString(), null, "Status: APPROVED");

        return getContentById(saved.getId());
    }

    @Override
    @Transactional
    public ContentResponse rejectContent(UUID id, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessValidationException("Rejection reason is required when rejecting content.");
        }

        ContentEntity entity = contentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", id));

        validateTransition(entity, ContentStatus.REJECTED);

        UUID currentUserId = SecurityUtils.getCurrentUser().map(u -> u.getUserId()).orElse(null);
        entity.setStatus(ContentStatus.REJECTED);
        entity.setReviewerId(currentUserId);
        entity.setRejectionReason(reason.trim());

        ContentEntity saved = contentRepository.save(entity);
        log.info("Rejected platform content: id={}, reason='{}'", saved.getId(), saved.getRejectionReason());

        auditService.logEvent("CONTENT_REJECTED", "CONTENT", saved.getId().toString(), null, "Reason: " + saved.getRejectionReason());

        return getContentById(saved.getId());
    }

    @Override
    @Transactional
    public ContentResponse scheduleContent(UUID id, Instant scheduledPublishAt) {
        if (scheduledPublishAt == null || !scheduledPublishAt.isAfter(Instant.now())) {
            throw new BusinessValidationException("Scheduled publication time must be in the future.");
        }

        ContentEntity entity = contentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", id));

        validateTransition(entity, ContentStatus.SCHEDULED);

        entity.setStatus(ContentStatus.SCHEDULED);
        entity.setScheduledPublishAt(scheduledPublishAt);

        ContentEntity saved = contentRepository.save(entity);
        log.info("Scheduled content for publication: id={}, scheduledPublishAt={}", saved.getId(), saved.getScheduledPublishAt());

        auditService.logEvent("CONTENT_SCHEDULED", "CONTENT", saved.getId().toString(), null, "Scheduled for: " + saved.getScheduledPublishAt());

        return getContentById(saved.getId());
    }

    @Override
    @Transactional
    public int publishScheduledContent() {
        Instant now = Instant.now();
        List<ContentEntity> readyItems = contentRepository.findReadyScheduledContent(now);
        if (readyItems.isEmpty()) {
            return 0;
        }

        int publishedCount = 0;
        for (ContentEntity item : readyItems) {
            item.setStatus(ContentStatus.PUBLISHED);
            item.setPublishedAt(now);
            item.setScheduledPublishAt(null);
            contentRepository.save(item);

            saveVersionSnapshot(item, "Automatic scheduled publication");
            auditService.logEvent("CONTENT_PUBLISHED", "CONTENT", item.getId().toString(), null, "Auto-published from schedule");
            publishedCount++;
            log.info("Auto-published scheduled content: id={}, title='{}'", item.getId(), item.getTitle());
        }

        return publishedCount;
    }

    @Override
    @Transactional
    public ContentResponse publishContent(UUID id) {
        ContentEntity entity = contentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", id));

        validateTransition(entity, ContentStatus.PUBLISHED);

        if (entity.getContentType() == ContentType.VIDEO && !StringUtils.hasText(entity.getYoutubeVideoId())) {
            throw new BusinessValidationException("Add a valid YouTube video before publishing.");
        }

        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setPublishedAt(Instant.now());
        entity.setScheduledPublishAt(null);

        ContentEntity saved = contentRepository.save(entity);
        saveVersionSnapshot(saved, "Published version " + (saved.getVersionNumber() != null ? saved.getVersionNumber() : 1));

        log.info("Published platform content: id={}, title='{}', publishedAt={}", saved.getId(), saved.getTitle(), saved.getPublishedAt());

        auditService.logEvent("CONTENT_PUBLISHED", "CONTENT", saved.getId().toString(), null, "Status: PUBLISHED");

        return getContentById(saved.getId());
    }

    @Override
    @Transactional
    public ContentResponse archiveContent(UUID id) {
        ContentEntity entity = contentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", id));

        validateTransition(entity, ContentStatus.ARCHIVED);

        entity.setStatus(ContentStatus.ARCHIVED);

        ContentEntity saved = contentRepository.save(entity);
        log.info("Archived platform content: id={}, title='{}'", saved.getId(), saved.getTitle());

        auditService.logEvent("CONTENT_ARCHIVED", "CONTENT", saved.getId().toString(), null, "Status: ARCHIVED");

        return getContentById(saved.getId());
    }

    @Override
    @Transactional
    public ContentResponse restoreContent(UUID id) {
        ContentEntity entity = contentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", id));

        if (entity.getStatus() != ContentStatus.ARCHIVED) {
            throw new BusinessValidationException("Only ARCHIVED content can be restored. Current status: " + entity.getStatus());
        }

        // Validate that attached tax services are still active
        if (entity.getTaxServices() != null) {
            for (TaxServiceEntity ts : entity.getTaxServices()) {
                if (ts != null && !Boolean.TRUE.equals(ts.getIsActive())) {
                    throw new BusinessValidationException("Cannot restore content with inactive Tax Service: " + ts.getName());
                }
            }
        }

        entity.setStatus(ContentStatus.DRAFT);
        ContentEntity saved = contentRepository.save(entity);
        log.info("Restored archived content to DRAFT: id={}, title='{}'", saved.getId(), saved.getTitle());

        auditService.logEvent("CONTENT_RESTORED", "CONTENT", saved.getId().toString(), null, "Restored to DRAFT");

        return getContentById(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ContentResponse previewContent(UUID id) {
        ContentEntity entity = contentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", id));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ContentDashboardStatsDto getDashboardStats() {
        long total = contentRepository.count();
        long published = contentRepository.countByStatus(ContentStatus.PUBLISHED);
        long draft = contentRepository.countByStatus(ContentStatus.DRAFT);
        long inReview = contentRepository.countByStatusIn(List.of(ContentStatus.SUBMITTED, ContentStatus.IN_REVIEW, ContentStatus.UNDER_REVIEW));
        long scheduled = contentRepository.countByStatus(ContentStatus.SCHEDULED);
        long archived = contentRepository.countByStatus(ContentStatus.ARCHIVED);
        long rejected = contentRepository.countByStatus(ContentStatus.REJECTED);

        // Build Needs Attention list
        List<ContentDashboardStatsDto.ContentAttentionItemDto> attentionList = new ArrayList<>();
        List<ContentEntity> reviewQueueItems = contentRepository.findReviewQueue(PageRequest.of(0, 5)).getContent();
        for (ContentEntity item : reviewQueueItems) {
            attentionList.add(ContentDashboardStatsDto.ContentAttentionItemDto.builder()
                    .id(item.getId().toString())
                    .title(item.getTitle())
                    .contentType(item.getContentType().name())
                    .status(item.getStatus().name())
                    .message("Waiting for peer / compliance review")
                    .updatedAt(item.getUpdatedAt())
                    .build());
        }

        // Build Recent Activity list
        List<ContentDashboardStatsDto.ContentActivityItemDto> recentActivity = new ArrayList<>();
        List<ContentEntity> recentlyUpdated = contentRepository.findRecentUpdated(PageRequest.of(0, 8));
        for (ContentEntity item : recentlyUpdated) {
            recentActivity.add(ContentDashboardStatsDto.ContentActivityItemDto.builder()
                    .id(item.getId().toString())
                    .action(item.getStatus().name())
                    .contentTitle(item.getTitle())
                    .contentType(item.getContentType().name())
                    .userName(item.getAuthor() != null ? item.getAuthor().getFirstName() : "Admin")
                    .timestamp(item.getUpdatedAt())
                    .build());
        }

        return ContentDashboardStatsDto.builder()
                .totalContent(total)
                .publishedCount(published)
                .draftCount(draft)
                .inReviewCount(inReview)
                .scheduledCount(scheduled)
                .archivedCount(archived)
                .rejectedCount(rejected)
                .needsAttention(attentionList)
                .recentActivity(recentActivity)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ContentSummaryResponse> getReviewQueue(int page, int size) {
        Page<ContentEntity> queuePage = contentRepository.findReviewQueue(PageRequest.of(page, size));
        return PagedResponse.of(queuePage, mapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentVersionDto> getVersionHistory(UUID contentId) {
        return contentVersionRepository.findByContentId(contentId, Sort.by(Sort.Direction.DESC, "versionNumber"))
                .stream()
                .map(this::toVersionDto)
                .collect(Collectors.toList());
    }

    private void saveVersionSnapshot(ContentEntity content, String changeSummary) {
        ContentVersionEntity version = ContentVersionEntity.builder()
                .contentId(content.getId())
                .versionNumber(content.getVersionNumber() != null ? content.getVersionNumber() : 1)
                .title(content.getTitle())
                .summary(content.getSummary())
                .body(content.getBody())
                .thumbnailUrl(content.getThumbnailUrl())
                .featuredImageUrl(content.getFeaturedImageUrl())
                .altText(content.getAltText())
                .status(content.getStatus())
                .changeSummary(changeSummary)
                .build();
        version.setCreatedBy(SecurityUtils.getCurrentUserEmail());
        contentVersionRepository.save(version);
    }

    private ContentVersionDto toVersionDto(ContentVersionEntity v) {
        return ContentVersionDto.builder()
                .id(v.getId())
                .contentId(v.getContentId())
                .versionNumber(v.getVersionNumber())
                .title(v.getTitle())
                .summary(v.getSummary())
                .body(v.getBody())
                .thumbnailUrl(v.getThumbnailUrl())
                .featuredImageUrl(v.getFeaturedImageUrl())
                .altText(v.getAltText())
                .status(v.getStatus())
                .changeSummary(v.getChangeSummary())
                .createdBy(v.getCreatedBy())
                .createdAt(v.getCreatedAt())
                .build();
    }

    // =========================================================================
    // Public / Customer Experience APIs (Strictly PUBLISHED content only)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ContentSummaryResponse> listPublicContent(ContentFilterRequest filterRequest) {
        // Enforce strictly PUBLISHED status for all public queries
        filterRequest.setStatus(ContentStatus.PUBLISHED);

        Pageable pageable = filterRequest.toPageable();
        Specification<ContentEntity> spec = createSpecification(filterRequest);

        Page<ContentEntity> page = contentRepository.findAll(spec, pageable);
        return PagedResponse.of(page, mapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ContentResponse getPublicContentBySlug(String slug) {
        if (!StringUtils.hasText(slug)) {
            throw new ResourceNotFoundException("Content", "slug", slug);
        }
        String cleanSlug = slug.trim().toLowerCase();

        // 1. Direct active match
        Optional<ContentEntity> directMatch = contentRepository.findBySlugAndStatusWithDetails(cleanSlug, ContentStatus.PUBLISHED);
        if (directMatch.isPresent()) {
            return mapper.toResponse(directMatch.get());
        }

        // 2. Check 301 alias redirect
        Optional<ContentSlugRedirectEntity> redirectOpt = contentSlugRedirectRepository.findByOldSlug(cleanSlug);
        if (redirectOpt.isPresent()) {
            String targetSlug = redirectOpt.get().getNewSlug();
            ContentEntity targetEntity = contentRepository.findBySlugAndStatusWithDetails(targetSlug, ContentStatus.PUBLISHED)
                    .orElseThrow(() -> new ResourceNotFoundException("Content", "slug", slug));
            ContentResponse resp = mapper.toResponse(targetEntity);
            resp.setRedirectSlug(targetSlug);
            return resp;
        }

        throw new ResourceNotFoundException("Content", "slug", slug);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentSummaryResponse> getRelatedPublicContent(String slug, int limit) {
        ContentEntity current = contentRepository.findBySlugAndStatusWithDetails(slug, ContentStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "slug", slug));

        Pageable pageable = PageRequest.of(0, Math.min(limit, 10));
        List<ContentEntity> related = contentRepository.findRelatedContent(
                ContentStatus.PUBLISHED,
                current.getCategoryId(),
                current.getId(),
                pageable
        );

        return related.stream()
                .map(mapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicContentCategoryDto> getPublicCategories() {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(cat -> PublicContentCategoryDto.builder()
                        .id(cat.getId())
                        .code(cat.getCode())
                        .name(cat.getName())
                        .description(cat.getDescription())
                        .publishedContentCount(contentRepository.countByStatusAndCategoryId(ContentStatus.PUBLISHED, cat.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    // =========================================================================
    // SEO, robots.txt, & XML Sitemap
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<PublicSitemapItemDto> getPublicSitemapItems() {
        List<ContentEntity> published = contentRepository.findByStatusAndScope(
                ContentStatus.PUBLISHED,
                ContentOwnershipScope.PLATFORM
        );

        return published.stream()
                .map(c -> {
                    Instant lastmod = c.getUpdatedAt() != null ? c.getUpdatedAt() : c.getPublishedAt();
                    return PublicSitemapItemDto.builder()
                            .loc("https://taxoryn.com/learn/" + c.getSlug())
                            .lastmod(lastmod != null ? lastmod : Instant.now())
                            .changefreq("weekly")
                            .priority(0.8)
                            .contentType(c.getContentType())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public String generateSitemapXml() {
        List<PublicSitemapItemDto> dynamicItems = getPublicSitemapItems();
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Static high-priority public routes
        appendSitemapUrl(sb, "https://taxoryn.com/learn", "daily", "1.0", null);
        appendSitemapUrl(sb, "https://taxoryn.com/learn/articles", "daily", "0.9", null);
        appendSitemapUrl(sb, "https://taxoryn.com/learn/videos", "daily", "0.9", null);
        appendSitemapUrl(sb, "https://taxoryn.com/learn/guides", "daily", "0.9", null);
        appendSitemapUrl(sb, "https://taxoryn.com/learn/faqs", "daily", "0.8", null);
        appendSitemapUrl(sb, "https://taxoryn.com/marketplace", "daily", "0.9", null);

        // Dynamic published articles, videos, guides, and FAQs
        for (PublicSitemapItemDto item : dynamicItems) {
            appendSitemapUrl(sb, item.getLoc(), item.getChangefreq(), String.valueOf(item.getPriority()), item.getLastmod());
        }

        sb.append("</urlset>\n");
        return sb.toString();
    }

    private void appendSitemapUrl(StringBuilder sb, String loc, String changefreq, String priority, Instant lastmod) {
        sb.append("  <url>\n");
        sb.append("    <loc>").append(loc).append("</loc>\n");
        if (lastmod != null) {
            sb.append("    <lastmod>").append(lastmod.toString()).append("</lastmod>\n");
        }
        if (changefreq != null) {
            sb.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        }
        if (priority != null) {
            sb.append("    <priority>").append(priority).append("</priority>\n");
        }
        sb.append("  </url>\n");
    }

    @Override
    public String getRobotsTxtContent() {
        return "User-agent: *\n" +
                "Allow: /learn\n" +
                "Allow: /learn/*\n" +
                "Allow: /marketplace\n" +
                "Allow: /marketplace/*\n" +
                "Allow: /api/v1/public/content\n" +
                "Allow: /api/v1/public/content/*\n" +
                "Allow: /api/v1/public/media/*\n" +
                "Disallow: /api/v1/admin/\n" +
                "Disallow: /api/v1/practice/\n" +
                "Disallow: /api/v1/portal/\n" +
                "Disallow: /api/v1/internal/\n" +
                "Disallow: /admin/\n" +
                "Disallow: /portal/\n\n" +
                "Sitemap: https://taxoryn.com/sitemap.xml\n";
    }

    private void validateTransition(ContentEntity entity, ContentStatus targetStatus) {
        if (!entity.getStatus().canTransitionTo(targetStatus)) {
            throw new BusinessValidationException("Cannot transition content from " + entity.getStatus() + " to " + targetStatus);
        }
    }

    private String normalizeSlug(String userSlug, String title) {
        if (StringUtils.hasText(userSlug)) {
            String clean = userSlug.trim().toLowerCase()
                    .replaceAll("[^a-z0-9-]+", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
            if (SLUG_PATTERN.matcher(clean).matches()) {
                return clean;
            }
        }

        String autoSlug = title.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        if (!StringUtils.hasText(autoSlug)) {
            autoSlug = "content-" + UUID.randomUUID().toString().substring(0, 8);
        }

        return autoSlug;
    }

    private Set<ContentTagEntity> resolveOrCreateTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptySet();
        }

        Set<ContentTagEntity> result = new HashSet<>();
        for (String raw : tagNames) {
            if (StringUtils.hasText(raw)) {
                String cleanName = raw.trim();
                String cleanSlug = cleanName.toLowerCase().replaceAll("[^a-z0-9-]+", "-");

                ContentTagEntity tag = tagRepository.findBySlug(cleanSlug)
                        .orElseGet(() -> tagRepository.save(
                                ContentTagEntity.builder()
                                        .name(cleanName)
                                        .slug(cleanSlug)
                                        .build()
                        ));
                result.add(tag);
            }
        }
        return result;
    }

    private Specification<ContentEntity> createSpecification(ContentFilterRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(request.getSearch())) {
                String term = "%" + request.getSearch().trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), term);
                Predicate summaryMatch = cb.like(cb.lower(root.get("summary")), term);
                Predicate slugMatch = cb.like(cb.lower(root.get("slug")), term);
                predicates.add(cb.or(titleMatch, summaryMatch, slugMatch));
            }

            if (request.getContentType() != null) {
                predicates.add(cb.equal(root.get("contentType"), request.getContentType()));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (request.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), request.getCategoryId()));
            }

            if (request.getTaxServiceId() != null) {
                Join<ContentEntity, TaxServiceEntity> taxServicesJoin = root.join("taxServices", JoinType.LEFT);
                Predicate primaryMatch = cb.equal(root.get("taxServiceId"), request.getTaxServiceId());
                Predicate mappedMatch = cb.equal(taxServicesJoin.get("id"), request.getTaxServiceId());
                predicates.add(cb.or(primaryMatch, mappedMatch));
                query.distinct(true);
            }

            if (request.getScope() != null) {
                predicates.add(cb.equal(root.get("scope"), request.getScope()));
            }

            if (StringUtils.hasText(request.getTag())) {
                Join<ContentEntity, ContentTagEntity> tagsJoin = root.join("tags", JoinType.INNER);
                predicates.add(cb.equal(cb.lower(tagsJoin.get("slug")), request.getTag().trim().toLowerCase()));
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
