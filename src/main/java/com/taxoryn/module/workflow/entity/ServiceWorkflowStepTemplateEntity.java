package com.taxoryn.module.workflow.entity;

import com.taxoryn.core.domain.BaseEntity;
import com.taxoryn.module.workflow.model.ServiceWorkType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Step definition template belonging to a ServiceWorkflowTemplate.
 */
@Entity
@Table(
        name = "service_workflow_step_templates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_swst_template_sequence", columnNames = {"workflow_template_id", "sequence"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceWorkflowStepTemplateEntity extends BaseEntity {

    @Column(name = "workflow_template_id", nullable = false)
    private UUID workflowTemplateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_template_id", insertable = false, updatable = false)
    private ServiceWorkflowTemplateEntity workflowTemplate;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", nullable = false, length = 50)
    private ServiceWorkType workType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "default_days_before_due_date")
    private Integer defaultDaysBeforeDueDate;

    @Column(name = "mandatory", nullable = false)
    @Builder.Default
    private boolean mandatory = true;

    @Column(name = "requires_client_input", nullable = false)
    @Builder.Default
    private boolean requiresClientInput = false;

    @Column(name = "requires_review", nullable = false)
    @Builder.Default
    private boolean requiresReview = false;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
