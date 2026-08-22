package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceLeadRepository extends JpaRepository<MarketplaceLeadEntity, UUID>, JpaSpecificationExecutor<MarketplaceLeadEntity> {

    List<MarketplaceLeadEntity> findAllByOrganizationId(UUID organizationId);

    Optional<MarketplaceLeadEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("SELECT l FROM MarketplaceLeadEntity l WHERE l.organizationId = :organizationId AND " +
           "(:status IS NULL OR l.leadStatus = :status) AND " +
           "(:search IS NULL OR LOWER(l.clientName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(l.clientEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(l.serviceCategory) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<MarketplaceLeadEntity> searchLeads(
            @Param("organizationId") UUID organizationId,
            @Param("status") MarketplaceLeadEntity.LeadStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    long countByOrganizationIdAndLeadStatus(UUID organizationId, MarketplaceLeadEntity.LeadStatus status);

    long countByOrganizationId(UUID organizationId);
}
