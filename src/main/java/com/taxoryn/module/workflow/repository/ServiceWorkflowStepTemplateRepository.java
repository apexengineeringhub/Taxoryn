package com.taxoryn.module.workflow.repository;

import com.taxoryn.module.workflow.entity.ServiceWorkflowStepTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceWorkflowStepTemplateRepository extends JpaRepository<ServiceWorkflowStepTemplateEntity, UUID> {

    List<ServiceWorkflowStepTemplateEntity> findAllByWorkflowTemplateIdAndActiveTrueOrderBySequenceAsc(UUID workflowTemplateId);
}
