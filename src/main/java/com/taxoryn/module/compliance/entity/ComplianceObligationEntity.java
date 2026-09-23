package com.taxoryn.module.compliance.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "compliance_obligations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceObligationEntity extends TenantAuditableEntity {

    public enum ComplianceStatus {
        PENDING, IN_PROGRESS, COMPLETED, OVERDUE, WAIVED, CANCELLED
    }

    public enum CompliancePriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "client_service_id")
    private UUID clientServiceId;

    @Column(name = "service_period_id")
    private UUID servicePeriodId;

    @Column(name = "workflow_id")
    private UUID workflowId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "obligation_type", length = 50)
    private ComplianceObligationType obligationType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "period_label", length = 100)
    private String periodLabel;

    @Column(name = "financial_year", length = 20)
    private String financialYear;

    @Column(name = "assessment_year", length = 20)
    private String assessmentYear;

    @Column(name = "statutory_due_date", nullable = false)
    private LocalDate statutoryDueDate;

    @Column(name = "internal_target_date")
    private LocalDate internalTargetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ComplianceObligationStatus status = ComplianceObligationStatus.UPCOMING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by", length = 255)
    private String completedBy;

    @Column(name = "filed_date")
    private LocalDate filedDate;

    @Column(name = "filed_at")
    private Instant filedAt;

    @Column(name = "filed_by", length = 255)
    private String filedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Legacy column mappings retained for database compatibility
    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "period", length = 50)
    private String period;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_type", length = 50)
    private ComplianceType complianceType;

    @Column(name = "gst_filing_id")
    private UUID gstFilingId;

    @Column(name = "itr_return_id")
    private UUID itrReturnId;

    @Column(name = "tds_return_id")
    private UUID tdsReturnId;

    public void setStatus(ComplianceStatus legacyStatus) {
        if (legacyStatus == null) return;
        this.status = switch (legacyStatus) {
            case PENDING -> ComplianceObligationStatus.UPCOMING;
            case IN_PROGRESS -> ComplianceObligationStatus.IN_PROGRESS;
            case COMPLETED -> ComplianceObligationStatus.COMPLETED;
            case OVERDUE -> ComplianceObligationStatus.OVERDUE;
            case WAIVED, CANCELLED -> ComplianceObligationStatus.CANCELLED;
        };
    }

    public void setStatus(ComplianceObligationStatus status) {
        this.status = status;
    }

    public void setPriority(CompliancePriority legacyPriority) {
        if (legacyPriority == null) return;
        this.priority = switch (legacyPriority) {
            case LOW -> TaskPriority.LOW;
            case MEDIUM -> TaskPriority.MEDIUM;
            case HIGH -> TaskPriority.HIGH;
            case CRITICAL -> TaskPriority.URGENT;
        };
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public void setComplianceType(ComplianceType complianceType) {
        this.complianceType = complianceType;
        if (complianceType != null && this.obligationType == null) {
            this.obligationType = switch (complianceType) {
                case GST -> ComplianceObligationType.GST_RETURN;
                case ITR -> ComplianceObligationType.ITR_FILING;
                case TDS -> ComplianceObligationType.TDS_RETURN;
                case ROC -> ComplianceObligationType.ROC_COMPLIANCE;
                case ADVANCE_TAX -> ComplianceObligationType.ADVANCE_TAX;
                default -> ComplianceObligationType.OTHER;
            };
        }
    }

    @PrePersist
    @PreUpdate
    public void syncLegacyFields() {
        if (this.statutoryDueDate != null) {
            this.dueDate = this.statutoryDueDate;
        } else if (this.dueDate != null) {
            this.statutoryDueDate = this.dueDate;
        }

        if (this.periodLabel != null) {
            this.period = this.periodLabel;
        } else if (this.period != null) {
            this.periodLabel = this.period;
        }

        if (this.obligationType != null && this.complianceType == null) {
            this.complianceType = switch (this.obligationType) {
                case GST_RETURN -> ComplianceType.GST;
                case ITR_FILING, TAX_AUDIT -> ComplianceType.ITR;
                case TDS_RETURN -> ComplianceType.TDS;
                case ROC_COMPLIANCE -> ComplianceType.ROC;
                case ADVANCE_TAX, SELF_ASSESSMENT_TAX -> ComplianceType.ADVANCE_TAX;
                default -> ComplianceType.OTHER;
            };
        }
    }

    public static class ComplianceObligationEntityBuilder {
        public ComplianceObligationEntityBuilder status(ComplianceStatus legacyStatus) {
            if (legacyStatus != null) {
                this.status$value = switch (legacyStatus) {
                    case PENDING -> ComplianceObligationStatus.UPCOMING;
                    case IN_PROGRESS -> ComplianceObligationStatus.IN_PROGRESS;
                    case COMPLETED -> ComplianceObligationStatus.COMPLETED;
                    case OVERDUE -> ComplianceObligationStatus.OVERDUE;
                    case WAIVED, CANCELLED -> ComplianceObligationStatus.CANCELLED;
                };
                this.status$set = true;
            }
            return this;
        }

        public ComplianceObligationEntityBuilder status(ComplianceObligationStatus status) {
            this.status$value = status;
            this.status$set = true;
            return this;
        }

        public ComplianceObligationEntityBuilder priority(CompliancePriority legacyPriority) {
            if (legacyPriority != null) {
                this.priority$value = switch (legacyPriority) {
                    case LOW -> TaskPriority.LOW;
                    case MEDIUM -> TaskPriority.MEDIUM;
                    case HIGH -> TaskPriority.HIGH;
                    case CRITICAL -> TaskPriority.URGENT;
                };
                this.priority$set = true;
            }
            return this;
        }

        public ComplianceObligationEntityBuilder priority(TaskPriority priority) {
            this.priority$value = priority;
            this.priority$set = true;
            return this;
        }

        public ComplianceObligationEntityBuilder complianceType(ComplianceType complianceType) {
            this.complianceType = complianceType;
            if (complianceType != null && this.obligationType == null) {
                this.obligationType = switch (complianceType) {
                    case GST -> ComplianceObligationType.GST_RETURN;
                    case ITR -> ComplianceObligationType.ITR_FILING;
                    case TDS -> ComplianceObligationType.TDS_RETURN;
                    case ROC -> ComplianceObligationType.ROC_COMPLIANCE;
                    case ADVANCE_TAX -> ComplianceObligationType.ADVANCE_TAX;
                    default -> ComplianceObligationType.OTHER;
                };
            }
            return this;
        }

        public ComplianceObligationEntityBuilder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            if (this.statutoryDueDate == null) {
                this.statutoryDueDate = dueDate;
            }
            return this;
        }

        public ComplianceObligationEntityBuilder statutoryDueDate(LocalDate statutoryDueDate) {
            this.statutoryDueDate = statutoryDueDate;
            if (this.dueDate == null) {
                this.dueDate = statutoryDueDate;
            }
            return this;
        }

        public ComplianceObligationEntityBuilder period(String period) {
            this.period = period;
            if (this.periodLabel == null) {
                this.periodLabel = period;
            }
            return this;
        }

        public ComplianceObligationEntityBuilder periodLabel(String periodLabel) {
            this.periodLabel = periodLabel;
            if (this.period == null) {
                this.period = periodLabel;
            }
            return this;
        }
    }
}

