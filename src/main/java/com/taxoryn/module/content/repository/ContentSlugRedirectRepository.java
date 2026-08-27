package com.taxoryn.module.content.repository;

import com.taxoryn.module.content.entity.ContentSlugRedirectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentSlugRedirectRepository extends JpaRepository<ContentSlugRedirectEntity, UUID> {

    Optional<ContentSlugRedirectEntity> findByOldSlug(String oldSlug);

    boolean existsByOldSlug(String oldSlug);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ContentSlugRedirectEntity r SET r.newSlug = :newSlug WHERE r.newSlug = :oldSlug")
    int flattenRedirectChains(@Param("oldSlug") String oldSlug, @Param("newSlug") String newSlug);
}
