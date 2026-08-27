package com.taxoryn.module.content.repository;

import com.taxoryn.module.content.entity.MediaAssetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAssetEntity, UUID> {

    @Query("SELECT m FROM MediaAssetEntity m WHERE " +
            "(:search IS NULL OR LOWER(m.filename) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.altText) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<MediaAssetEntity> searchMediaAssets(@Param("search") String search, Pageable pageable);
}
