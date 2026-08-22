package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.MarketplaceServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarketplaceServiceRepository extends JpaRepository<MarketplaceServiceEntity, UUID> {

    List<MarketplaceServiceEntity> findByMarketplaceProfileIdAndIsActiveTrue(UUID marketplaceProfileId);

    List<MarketplaceServiceEntity> findByOrganizationId(UUID organizationId);

    List<MarketplaceServiceEntity> findByOrganizationIdAndIsActiveTrue(UUID organizationId);
}
