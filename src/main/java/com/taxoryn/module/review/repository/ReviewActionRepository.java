package com.taxoryn.module.review.repository;

import com.taxoryn.module.review.entity.ReviewActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewActionRepository extends JpaRepository<ReviewActionEntity, UUID> {
    List<ReviewActionEntity> findByOrganizationIdAndReviewRequestIdOrderByOccurredAtAsc(UUID organizationId, UUID reviewRequestId);
}
