package com.taxoryn.module.workflow.entity;

import com.taxoryn.core.domain.AuditableEntity;
import com.taxoryn.module.client.entity.ClientServiceType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Defines a reusable operational workflow template for a specific client service engagement type.
 * When organizationId is null, it represents a global system default template.
 */
@Entity
@Table(name = "service_workflow_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceWorkflowTemplateEntity extends AuditableEntity {

    @Column(name = "organization_id")
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 100)
    private ClientServiceType serviceType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_system_default", nullable = false)
    @Builder.Default
    private boolean isSystemDefault = false;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "workflowTemplate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    @Builder.Default
    private List<ServiceWorkflowStepTemplateEntity> stepTemplates = new ArrayList<>();
}
