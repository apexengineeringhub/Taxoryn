package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceVerificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceVerificationRepository extends JpaRepository<MarketplaceVerificationEntity, UUID> {

    List<MarketplaceVerificationEntity> findByOrganizationId(UUID organizationId);

    Optional<MarketplaceVerificationEntity> findTopByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Page<MarketplaceVerificationEntity> findByVerificationStatus(
            MarketplaceProfileEntity.VerificationStatus status,
            Pageable pageable
    );
}
