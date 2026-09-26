package com.taxoryn.module.moduleconfig.repository;

import com.taxoryn.module.moduleconfig.entity.ProductFeatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductFeatureRepository extends JpaRepository<ProductFeatureEntity, UUID> {

    List<ProductFeatureEntity> findByModuleCodeOrderByDisplayOrderAsc(String moduleCode);

    Optional<ProductFeatureEntity> findByModuleCodeAndCode(String moduleCode, String code);

    List<ProductFeatureEntity> findAllByOrderByDisplayOrderAsc();

    boolean existsByModuleCodeAndCode(String moduleCode, String code);
}
