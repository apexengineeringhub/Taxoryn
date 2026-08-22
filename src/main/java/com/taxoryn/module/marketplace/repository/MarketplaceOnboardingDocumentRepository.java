package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.MarketplaceOnboardingDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarketplaceOnboardingDocumentRepository extends JpaRepository<MarketplaceOnboardingDocumentEntity, UUID> {

    List<MarketplaceOnboardingDocumentEntity> findByOnboardingIdOrderByCreatedAtAsc(UUID onboardingId);

    List<MarketplaceOnboardingDocumentEntity> findByOnboardingIdAndVerificationStatus(
            UUID onboardingId,
            MarketplaceOnboardingDocumentEntity.VerificationStatus status
    );
}
