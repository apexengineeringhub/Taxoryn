package com.taxoryn.module.worktemplate.entity;

import com.taxoryn.core.domain.AuditableEntity;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.worktemplate.model.RecurrenceType;
import com.taxoryn.module.worktemplate.model.WorkTemplateStatus;
import com.taxoryn.module.worktemplate.model.WorkTemplateType;
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
 * Reusable definition of practice work belonging to a Service.
 */
@Entity
@Table(name = "work_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkTemplateEntity extends AuditableEntity {

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "template_code", nullable = false, length = 100)
    private String templateCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private ServiceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private WorkTemplateStatus status = WorkTemplateStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 50)
    @Builder.Default
    private WorkTemplateType templateType = WorkTemplateType.STATUTORY_COMPLIANCE;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 50)
    @Builder.Default
    private RecurrenceType recurrenceType = RecurrenceType.MONTHLY;

    @Column(name = "recurrence_interval", nullable = false)
    @Builder.Default
    private int recurrenceInterval = 1;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Column(name = "month_of_year")
    private Integer monthOfYear;

    @Column(name = "recurrence_enabled", nullable = false)
    @Builder.Default
    private boolean recurrenceEnabled = true;

    @Column(name = "is_system_default", nullable = false)
    @Builder.Default
    private boolean isSystemDefault = false;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequenceOrder ASC")
    @Builder.Default
    private List<WorkTemplateTaskEntity> tasks = new ArrayList<>();
}
