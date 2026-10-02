package com.taxoryn.module.review.repository;

import com.taxoryn.module.review.entity.ReviewRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRequestRepository extends JpaRepository<ReviewRequestEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReviewRequestEntity r where r.id = :id and r.organizationId = :organizationId")
    Optional<ReviewRequestEntity> findForUpdate(@Param("id") UUID id, @Param("organizationId") UUID organizationId);
    Optional<ReviewRequestEntity> findFirstByOrganizationIdAndResourceTypeAndResourceIdAndStatusInOrderByRequestedAtDesc(
            UUID organizationId, String resourceType, UUID resourceId, List<com.taxoryn.module.review.entity.ReviewStatus> statuses);
    Optional<ReviewRequestEntity> findFirstByOrganizationIdAndResourceTypeAndResourceIdOrderByRequestedAtDesc(
            UUID organizationId, String resourceType, UUID resourceId);
    List<ReviewRequestEntity> findTop100ByOrganizationIdOrderByRequestedAtDesc(UUID organizationId);
}
