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
import com.taxoryn.module.content.repository.ContentTagRepository;
import com.taxoryn.module.marketplace.entity.TaxServiceCategoryEntity;
import com.taxoryn.module.marketplace.entity.TaxServiceEntity;
import com.taxoryn.module.marketplace.repository.TaxServiceCategoryRepository;
import com.taxoryn.module.marketplace.repository.TaxServiceRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        TaxServiceEntity taxService = null;
        if (request.getTaxServiceId() != null) {
            taxService = taxServiceRepository.findById(request.getTaxServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("TaxService", "id", request.getTaxServiceId()));
        }

        UUID currentUserId = SecurityUtils.getCurrentUser().map(u -> u.getUserId()).orElse(null);

        Set<ContentTagEntity> resolvedTags = resolveOrCreateTags(request.getTags());

        ContentEntity entity = ContentEntity.builder()
                .contentType(request.getContentType())
                .title(request.getTitle().trim())
                .slug(slug)
                .summary(request.getSummary() != null ? request.getSummary().trim() : null)
                .body(request.getBody())
                .thumbnailUrl(request.getThumbnailUrl())
                .status(ContentStatus.DRAFT)
                .categoryId(request.getCategoryId())
                .category(category)
                .taxServiceId(request.getTaxServiceId())
                .taxService(taxService)
                .scope(request.getScope() != null ? request.getScope() : ContentOwnershipScope.PLATFORM)
                .authorId(currentUserId)
                .tags(resolvedTags)
                .build();

        ContentEntity saved = contentRepository.save(entity);
        log.info("Created platform content: id={}, title='{}', type={}, slug='{}'", saved.getId(), saved.getTitle(), saved.getContentType(), saved.getSlug());

        auditService.logEvent("CONTENT_CREATED", "CONTENT", saved.getId().toString(), null, saved.getTitle() + " (" + saved.getContentType() + ")");

        return mapper.toResponse(saved);
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
        ContentEntity entity = contentRepository.findBySlugWithDetails(slug.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Content", "slug", slug));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public ContentResponse updateContent(UUID id, UpdateContentRequest request) {
        ContentEntity entity = contentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", id));

        if (StringUtils.hasText(request.getTitle())) {
            entity.setTitle(request.getTitle().trim());
        }

        if (request.getContentType() != null) {
            entity.setContentType(request.getContentType());
        }

        if (StringUtils.hasText(request.getSlug())) {
            String newSlug = normalizeSlug(request.getSlug(), entity.getTitle());
            if (contentRepository.existsBySlugAndIdNot(newSlug, id)) {
                throw new DuplicateResourceException("Content with slug '" + newSlug + "' already exists");
            }
            entity.setSlug(newSlug);
        }

        if (request.getSummary() != null) {
            entity.setSummary(request.getSummary().trim());
        }

        if (StringUtils.hasText(request.getBody())) {
            entity.setBody(request.getBody());
        }

        if (request.getThumbnailUrl() != null) {
            entity.setThumbnailUrl(request.getThumbnailUrl().trim());
        }

        if (request.getCategoryId() != null) {
            TaxServiceCategoryEntity cat = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("TaxServiceCategory", "id", request.getCategoryId()));
            entity.setCategoryId(request.getCategoryId());
            entity.setCategory(cat);
        }

        if (request.getTaxServiceId() != null) {
            TaxServiceEntity svc = taxServiceRepository.findById(request.getTaxServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("TaxService", "id", request.getTaxServiceId()));
            entity.setTaxServiceId(request.getTaxServiceId());
            entity.setTaxService(svc);
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

        validateTransition(entity, ContentStatus.UNDER_REVIEW);

        entity.setStatus(ContentStatus.UNDER_REVIEW);
        ContentEntity saved = contentRepository.save(entity);
        log.info("Submitted content for review: id={}, title='{}'", saved.getId(), saved.getTitle());

        auditService.logEvent("CONTENT_SUBMITTED_FOR_REVIEW", "CONTENT", saved.getId().toString(), null, "Status: UNDER_REVIEW");

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

        ContentEntity saved = contentRepository.save(entity);
        log.info("Approved platform content: id={}, title='{}', reviewer={}", saved.getId(), saved.getTitle(), currentUserId);

        auditService.logEvent("CONTENT_APPROVED", "CONTENT", saved.getId().toString(), null, "Status: APPROVED");

        return getContentById(saved.getId());
    }

    @Override
    @Transactional
    public ContentResponse publishContent(UUID id) {
        ContentEntity entity = contentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", id));

        validateTransition(entity, ContentStatus.PUBLISHED);

        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setPublishedAt(Instant.now());

        ContentEntity saved = contentRepository.save(entity);
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
        ContentEntity entity = contentRepository.findBySlugAndStatusWithDetails(slug.toLowerCase().trim(), ContentStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "slug", slug));

        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentSummaryResponse> getRelatedPublicContent(String slug, int limit) {
        ContentEntity current = contentRepository.findBySlugAndStatusWithDetails(slug.toLowerCase().trim(), ContentStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "slug", slug));

        int effectiveLimit = limit > 0 ? Math.min(limit, 12) : 4;
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, effectiveLimit);

        List<ContentEntity> sameCategory = current.getCategoryId() != null
                ? contentRepository.findRelatedContent(ContentStatus.PUBLISHED, current.getCategoryId(), current.getId(), pageable)
                : Collections.emptyList();

        if (sameCategory.size() >= effectiveLimit) {
            return sameCategory.stream().map(mapper::toSummaryResponse).collect(Collectors.toList());
        }

        java.util.Set<UUID> seenIds = sameCategory.stream().map(ContentEntity::getId).collect(Collectors.toSet());
        seenIds.add(current.getId());

        List<ContentEntity> fallback = contentRepository.findRelatedContent(
                ContentStatus.PUBLISHED,
                null,
                current.getId(),
                pageable
        );

        java.util.List<ContentEntity> combined = new java.util.ArrayList<>(sameCategory);
        for (ContentEntity entity : fallback) {
            if (combined.size() >= effectiveLimit) break;
            if (!seenIds.contains(entity.getId())) {
                combined.add(entity);
                seenIds.add(entity.getId());
            }
        }

        return combined.stream()
                .map(mapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicContentCategoryDto> getPublicCategories() {
        List<TaxServiceCategoryEntity> categories = categoryRepository.findAllByOrderBySortOrderAsc();
        return categories.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .map(cat -> {
                    long count = contentRepository.countByStatusAndCategoryId(ContentStatus.PUBLISHED, cat.getId());
                    return PublicContentCategoryDto.builder()
                            .id(cat.getId())
                            .code(cat.getCode())
                            .name(cat.getName())
                            .description(cat.getDescription())
                            .icon(cat.getIcon())
                            .publishedContentCount(count)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private void validateTransition(ContentEntity entity, ContentStatus targetStatus) {
        if (!entity.getStatus().canTransitionTo(targetStatus)) {
            throw new BusinessValidationException(
                    String.format("Invalid content lifecycle transition: cannot change status from '%s' to '%s'",
                            entity.getStatus(), targetStatus));
        }
    }

    private String normalizeSlug(String customSlug, String title) {
        String base = StringUtils.hasText(customSlug) ? customSlug : title;
        if (!StringUtils.hasText(base)) {
            throw new BusinessValidationException("Slug or title is required to derive slug");
        }

        String cleaned = base.toLowerCase().trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        if (cleaned.startsWith("-")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.endsWith("-")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        if (!StringUtils.hasText(cleaned)) {
            throw new BusinessValidationException("Derived slug is empty or invalid: " + base);
        }

        if (!SLUG_PATTERN.matcher(cleaned).matches()) {
            throw new BusinessValidationException("Slug contains invalid format. Only alphanumeric characters and hyphens are allowed: " + cleaned);
        }

        return cleaned;
    }

    private Set<ContentTagEntity> resolveOrCreateTags(Set<String> tagInputs) {
        if (tagInputs == null || tagInputs.isEmpty()) {
            return new HashSet<>();
        }

        Set<ContentTagEntity> resolved = new HashSet<>();
        for (String input : tagInputs) {
            if (!StringUtils.hasText(input)) continue;
            String trimmed = input.trim();
            String slug = trimmed.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
            if (!StringUtils.hasText(slug)) continue;

            ContentTagEntity tag = tagRepository.findBySlug(slug)
                    .orElseGet(() -> tagRepository.save(ContentTagEntity.builder()
                            .name(trimmed)
                            .slug(slug)
                            .build()));
            resolved.add(tag);
        }
        return resolved;
    }

    private Specification<ContentEntity> createSpecification(ContentFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getContentType() != null) {
                predicates.add(cb.equal(root.get("contentType"), filter.getContentType()));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), filter.getCategoryId()));
            }

            if (filter.getTaxServiceId() != null) {
                predicates.add(cb.equal(root.get("taxServiceId"), filter.getTaxServiceId()));
            }

            if (filter.getAuthorId() != null) {
                predicates.add(cb.equal(root.get("authorId"), filter.getAuthorId()));
            }

            if (filter.getReviewerId() != null) {
                predicates.add(cb.equal(root.get("reviewerId"), filter.getReviewerId()));
            }

            if (StringUtils.hasText(filter.getTag())) {
                Join<ContentEntity, ContentTagEntity> tagJoin = root.join("tags", JoinType.INNER);
                String tagLower = filter.getTag().trim().toLowerCase();
                predicates.add(cb.or(
                        cb.equal(cb.lower(tagJoin.get("slug")), tagLower),
                        cb.equal(cb.lower(tagJoin.get("name")), tagLower)
                ));
            }

            if (StringUtils.hasText(filter.getSearch())) {
                String searchPattern = "%" + filter.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), searchPattern),
                        cb.like(cb.lower(root.get("slug")), searchPattern),
                        cb.like(cb.lower(root.get("summary")), searchPattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
