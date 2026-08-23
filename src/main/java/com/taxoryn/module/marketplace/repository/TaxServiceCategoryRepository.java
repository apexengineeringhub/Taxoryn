package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.TaxServiceCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxServiceCategoryRepository extends JpaRepository<TaxServiceCategoryEntity, UUID>, JpaSpecificationExecutor<TaxServiceCategoryEntity> {

    Optional<TaxServiceCategoryEntity> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<TaxServiceCategoryEntity> findByIsActiveTrueOrderBySortOrderAsc();

    List<TaxServiceCategoryEntity> findAllByOrderBySortOrderAsc();
}
