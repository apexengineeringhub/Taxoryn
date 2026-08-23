package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.TaxServiceAliasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxServiceAliasRepository extends JpaRepository<TaxServiceAliasEntity, UUID> {

    List<TaxServiceAliasEntity> findByTaxServiceIdAndIsActiveTrue(UUID taxServiceId);

    List<TaxServiceAliasEntity> findByTaxServiceId(UUID taxServiceId);

    Optional<TaxServiceAliasEntity> findByNormalizedAliasAndIsActiveTrue(String normalizedAlias);

    @Query("SELECT a FROM TaxServiceAliasEntity a JOIN FETCH a.taxService s WHERE a.normalizedAlias = :normalized AND a.isActive = true AND s.isActive = true")
    List<TaxServiceAliasEntity> findActiveMatchingAliases(@Param("normalized") String normalized);

    @Query("SELECT a FROM TaxServiceAliasEntity a JOIN FETCH a.taxService s WHERE LOWER(a.normalizedAlias) LIKE LOWER(CONCAT('%', :normalized, '%')) AND a.isActive = true AND s.isActive = true")
    List<TaxServiceAliasEntity> searchActiveAliases(@Param("normalized") String normalized);
}
