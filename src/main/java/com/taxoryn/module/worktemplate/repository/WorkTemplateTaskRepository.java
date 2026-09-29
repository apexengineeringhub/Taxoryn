package com.taxoryn.module.worktemplate.repository;

import com.taxoryn.module.worktemplate.entity.WorkTemplateTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkTemplateTaskRepository extends JpaRepository<WorkTemplateTaskEntity, UUID> {

    List<WorkTemplateTaskEntity> findAllByTemplateIdOrderBySequenceOrderAsc(UUID templateId);

    Optional<WorkTemplateTaskEntity> findByIdAndTemplateId(UUID id, UUID templateId);

    @Query("SELECT COALESCE(MAX(t.sequenceOrder), 0) FROM WorkTemplateTaskEntity t WHERE t.templateId = :templateId")
    int findMaxSequenceOrderByTemplateId(@Param("templateId") UUID templateId);

    boolean existsByTemplateIdAndSequenceOrder(UUID templateId, int sequenceOrder);

    void deleteAllByTemplateId(UUID templateId);
}
