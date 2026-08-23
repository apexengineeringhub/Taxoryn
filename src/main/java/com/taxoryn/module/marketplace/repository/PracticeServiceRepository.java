package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.PracticeServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PracticeServiceRepository extends JpaRepository<PracticeServiceEntity, UUID> {

    @Query("SELECT ps FROM PracticeServiceEntity ps JOIN FETCH ps.taxService ts JOIN FETCH ts.category c WHERE ps.marketplaceProfileId = :marketplaceProfileId AND ps.isActive = true AND ts.isActive = true ORDER BY c.sortOrder ASC, ts.sortOrder ASC")
    List<PracticeServiceEntity> findByMarketplaceProfileIdAndIsActiveTrue(@Param("marketplaceProfileId") UUID marketplaceProfileId);

    List<PracticeServiceEntity> findByMarketplaceProfileId(UUID marketplaceProfileId);

    List<PracticeServiceEntity> findByOrganizationIdAndIsActiveTrue(UUID organizationId);

    Optional<PracticeServiceEntity> findByMarketplaceProfileIdAndTaxServiceId(UUID marketplaceProfileId, UUID taxServiceId);

    boolean existsByMarketplaceProfileIdAndTaxServiceIdAndIsActiveTrue(UUID marketplaceProfileId, UUID taxServiceId);

    @Query("SELECT ps.marketplaceProfileId FROM PracticeServiceEntity ps WHERE ps.taxServiceId = :taxServiceId AND ps.isActive = true")
    List<UUID> findProfileIdsOfferingTaxService(@Param("taxServiceId") UUID taxServiceId);

    @Query("SELECT ps.marketplaceProfileId FROM PracticeServiceEntity ps JOIN ps.taxService ts WHERE ts.code = :code AND ps.isActive = true AND ts.isActive = true")
    List<UUID> findProfileIdsOfferingTaxServiceCode(@Param("code") String code);

    @Query("SELECT ps.marketplaceProfileId FROM PracticeServiceEntity ps JOIN ps.taxService ts WHERE ts.categoryId = :categoryId AND ps.isActive = true AND ts.isActive = true")
    List<UUID> findProfileIdsOfferingCategory(@Param("categoryId") UUID categoryId);

    @Query("SELECT ps.marketplaceProfileId FROM PracticeServiceEntity ps JOIN ps.taxService ts JOIN ts.category c WHERE c.code = :categoryCode AND ps.isActive = true AND ts.isActive = true")
    List<UUID> findProfileIdsOfferingCategoryCode(@Param("categoryCode") String categoryCode);

    @Query("SELECT ps FROM PracticeServiceEntity ps JOIN FETCH ps.taxService ts JOIN FETCH ts.category c WHERE ps.marketplaceProfileId IN :profileIds AND ps.isActive = true AND ts.isActive = true ORDER BY c.sortOrder ASC, ts.sortOrder ASC")
    List<PracticeServiceEntity> findActiveServicesForProfiles(@Param("profileIds") Collection<UUID> profileIds);
}
