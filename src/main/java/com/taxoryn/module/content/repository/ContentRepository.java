package com.taxoryn.module.content.repository;

import com.taxoryn.module.content.entity.ContentEntity;
import com.taxoryn.module.content.entity.ContentStatus;
import com.taxoryn.module.content.entity.ContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<ContentEntity, UUID>, JpaSpecificationExecutor<ContentEntity> {

    @Query("SELECT c FROM ContentEntity c LEFT JOIN FETCH c.tags LEFT JOIN FETCH c.taxServices LEFT JOIN FETCH c.category LEFT JOIN FETCH c.taxService LEFT JOIN FETCH c.author LEFT JOIN FETCH c.reviewer WHERE c.id = :id")
    Optional<ContentEntity> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT c FROM ContentEntity c LEFT JOIN FETCH c.tags LEFT JOIN FETCH c.taxServices LEFT JOIN FETCH c.category LEFT JOIN FETCH c.taxService LEFT JOIN FETCH c.author LEFT JOIN FETCH c.reviewer WHERE c.slug = :slug")
    Optional<ContentEntity> findBySlugWithDetails(@Param("slug") String slug);

    @Query("SELECT c FROM ContentEntity c LEFT JOIN FETCH c.tags LEFT JOIN FETCH c.taxServices LEFT JOIN FETCH c.category LEFT JOIN FETCH c.taxService LEFT JOIN FETCH c.author LEFT JOIN FETCH c.reviewer WHERE c.slug = :slug AND c.status = :status")
    Optional<ContentEntity> findBySlugAndStatusWithDetails(@Param("slug") String slug, @Param("status") ContentStatus status);

    Optional<ContentEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    long countByStatus(ContentStatus status);

    long countByContentType(ContentType contentType);

    long countByStatusAndCategoryId(ContentStatus status, UUID categoryId);

    @Query("SELECT c FROM ContentEntity c WHERE c.status = :status AND c.id <> :id AND (:categoryId IS NULL OR c.categoryId = :categoryId) ORDER BY c.publishedAt DESC NULLS LAST")
    List<ContentEntity> findRelatedContent(@Param("status") ContentStatus status, @Param("categoryId") UUID categoryId, @Param("id") UUID id, Pageable pageable);
}
