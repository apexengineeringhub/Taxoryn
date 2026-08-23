package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.PracticeLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PracticeLocationRepository extends JpaRepository<PracticeLocationEntity, UUID>, JpaSpecificationExecutor<PracticeLocationEntity> {

    List<PracticeLocationEntity> findByMarketplaceProfileIdOrderByIsPrimaryDescCreatedAtAsc(UUID marketplaceProfileId);

    List<PracticeLocationEntity> findByMarketplaceProfileIdAndIsActiveTrueOrderByIsPrimaryDescCreatedAtAsc(UUID marketplaceProfileId);

    Optional<PracticeLocationEntity> findByIdAndMarketplaceProfileId(UUID id, UUID marketplaceProfileId);

    Optional<PracticeLocationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<PracticeLocationEntity> findByMarketplaceProfileIdAndIsPrimaryTrue(UUID marketplaceProfileId);

    long countByMarketplaceProfileIdAndIsActiveTrue(UUID marketplaceProfileId);

    long countByMarketplaceProfileId(UUID marketplaceProfileId);

    boolean existsByMarketplaceProfileIdAndCityIgnoreCaseAndAddressLine1IgnoreCase(UUID marketplaceProfileId, String city, String addressLine1);

    List<PracticeLocationEntity> findByOrganizationId(UUID organizationId);

    List<PracticeLocationEntity> findByCityIgnoreCaseAndIsActiveTrue(String city);

    @Modifying
    @Query("UPDATE PracticeLocationEntity l SET l.isPrimary = false WHERE l.marketplaceProfileId = :profileId AND l.id <> :excludeLocationId")
    void clearOtherPrimaryLocations(@Param("profileId") UUID profileId, @Param("excludeLocationId") UUID excludeLocationId);

    @Modifying
    @Query("UPDATE PracticeLocationEntity l SET l.isPrimary = false WHERE l.marketplaceProfileId = :profileId")
    void clearAllPrimaryLocations(@Param("profileId") UUID profileId);

    @Query("SELECT l FROM PracticeLocationEntity l " +
           "WHERE l.isActive = true " +
           "AND l.latitude IS NOT NULL AND l.longitude IS NOT NULL " +
           "AND l.latitude >= :minLat AND l.latitude <= :maxLat " +
           "AND l.longitude >= :minLng AND l.longitude <= :maxLng")
    List<PracticeLocationEntity> findActiveLocationsInBoundingBox(
            @Param("minLat") java.math.BigDecimal minLat,
            @Param("maxLat") java.math.BigDecimal maxLat,
            @Param("minLng") java.math.BigDecimal minLng,
            @Param("maxLng") java.math.BigDecimal maxLng
    );
}
