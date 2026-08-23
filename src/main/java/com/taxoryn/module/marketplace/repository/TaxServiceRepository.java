package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.TaxServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxServiceRepository extends JpaRepository<TaxServiceEntity, UUID>, JpaSpecificationExecutor<TaxServiceEntity> {

    Optional<TaxServiceEntity> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<TaxServiceEntity> findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(UUID categoryId);

    List<TaxServiceEntity> findByIsActiveTrueOrderBySortOrderAsc();

    List<TaxServiceEntity> findByIdInAndIsActiveTrue(Collection<UUID> ids);

    List<TaxServiceEntity> findByCodeInIgnoreCase(Collection<String> codes);

    @Query("SELECT s FROM TaxServiceEntity s JOIN FETCH s.category c WHERE s.isActive = true AND c.isActive = true ORDER BY c.sortOrder ASC, s.sortOrder ASC")
    List<TaxServiceEntity> findAllActiveWithCategory();

    @Query("SELECT s FROM TaxServiceEntity s WHERE s.isActive = true AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.code) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<TaxServiceEntity> searchActiveByNameOrCode(@Param("query") String query);
}
