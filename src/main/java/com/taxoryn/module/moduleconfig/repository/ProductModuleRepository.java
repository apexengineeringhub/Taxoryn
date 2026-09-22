package com.taxoryn.module.moduleconfig.repository;

import com.taxoryn.module.moduleconfig.entity.ProductModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCategory;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductModuleRepository extends JpaRepository<ProductModuleEntity, UUID> {

    Optional<ProductModuleEntity> findByCode(ProductModuleCode code);

    List<ProductModuleEntity> findAllByOrderByDisplayOrderAsc();

    List<ProductModuleEntity> findByCategoryOrderByDisplayOrderAsc(ProductModuleCategory category);
}
