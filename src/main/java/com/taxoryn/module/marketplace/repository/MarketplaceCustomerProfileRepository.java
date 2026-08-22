package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.MarketplaceCustomerProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceCustomerProfileRepository extends JpaRepository<MarketplaceCustomerProfileEntity, UUID> {

    Optional<MarketplaceCustomerProfileEntity> findByUserId(UUID userId);

    Optional<MarketplaceCustomerProfileEntity> findByEmailIgnoreCase(String email);

    boolean existsByUserId(UUID userId);

    boolean existsByEmailIgnoreCase(String email);
}
