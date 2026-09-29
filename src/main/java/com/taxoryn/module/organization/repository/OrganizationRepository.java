package com.taxoryn.module.organization.repository;

import com.taxoryn.module.organization.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {

    Optional<OrganizationEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByStatus(OrganizationEntity.OrganizationStatus status);

    long countByCreatedAtAfter(java.time.Instant timestamp);

    @org.springframework.data.jpa.repository.Query("SELECT o.subscriptionPlan, COUNT(o) FROM OrganizationEntity o GROUP BY o.subscriptionPlan")
    java.util.List<Object[]> countBySubscriptionPlanGrouped();

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT o FROM OrganizationEntity o WHERE " +
                    "(:status IS NULL OR o.status = :status) AND " +
                    "(:search IS NULL OR (" +
                    "  LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR LOWER(o.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR (o.phone IS NOT NULL AND o.phone LIKE CONCAT('%', :search, '%')) " +
                    "  OR (o.city IS NOT NULL AND LOWER(o.city) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "  OR EXISTS (" +
                    "    SELECT 1 FROM UserEntity u WHERE u.organizationId = o.id AND (" +
                    "      LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "      OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "      OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))" +
                    "    )" +
                    "  )" +
                    "))",
            countQuery = "SELECT COUNT(o) FROM OrganizationEntity o WHERE " +
                    "(:status IS NULL OR o.status = :status) AND " +
                    "(:search IS NULL OR (" +
                    "  LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR LOWER(o.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR (o.phone IS NOT NULL AND o.phone LIKE CONCAT('%', :search, '%')) " +
                    "  OR (o.city IS NOT NULL AND LOWER(o.city) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "  OR EXISTS (" +
                    "    SELECT 1 FROM UserEntity u WHERE u.organizationId = o.id AND (" +
                    "      LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "      OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "      OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))" +
                    "    )" +
                    "  )" +
                    "))"
    )
    org.springframework.data.domain.Page<OrganizationEntity> findPracticesWithFilters(
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("status") OrganizationEntity.OrganizationStatus status,
            org.springframework.data.domain.Pageable pageable
    );
}
