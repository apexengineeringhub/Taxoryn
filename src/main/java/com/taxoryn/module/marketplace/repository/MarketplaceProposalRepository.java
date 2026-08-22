package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.MarketplaceProposalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceProposalRepository extends JpaRepository<MarketplaceProposalEntity, UUID> {

    List<MarketplaceProposalEntity> findByLeadId(UUID leadId);

    Optional<MarketplaceProposalEntity> findByAccessToken(String accessToken);

    Page<MarketplaceProposalEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Optional<MarketplaceProposalEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
