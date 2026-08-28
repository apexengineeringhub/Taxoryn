package com.taxoryn.module.content.repository;

import com.taxoryn.module.content.entity.ContentVersionEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentVersionRepository extends JpaRepository<ContentVersionEntity, UUID> {

    List<ContentVersionEntity> findByContentId(UUID contentId, Sort sort);

    Optional<ContentVersionEntity> findByContentIdAndVersionNumber(UUID contentId, Integer versionNumber);

    long countByContentId(UUID contentId);
}
