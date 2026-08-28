package com.taxoryn.module.content.repository;

import com.taxoryn.module.content.entity.ContentTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentTagRepository extends JpaRepository<ContentTagEntity, UUID> {

    Optional<ContentTagEntity> findBySlug(String slug);

    Optional<ContentTagEntity> findByNameIgnoreCase(String name);

    List<ContentTagEntity> findBySlugIn(Collection<String> slugs);

    List<ContentTagEntity> findByNameInIgnoreCase(Collection<String> names);
}
