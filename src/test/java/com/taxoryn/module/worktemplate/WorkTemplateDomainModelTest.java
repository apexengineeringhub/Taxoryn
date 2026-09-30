package com.taxoryn.module.worktemplate;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.worktemplate.entity.EngagementWorkTemplateEntity;
import com.taxoryn.module.worktemplate.entity.WorkInstanceEntity;
import com.taxoryn.module.worktemplate.entity.WorkTemplateEntity;
import com.taxoryn.module.worktemplate.entity.WorkTemplateTaskEntity;
import com.taxoryn.module.worktemplate.model.RecurrenceType;
import com.taxoryn.module.worktemplate.model.WorkInstanceStatus;
import com.taxoryn.module.worktemplate.model.WorkTemplateStatus;
import com.taxoryn.module.worktemplate.model.WorkTemplateType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WorkTemplateDomainModelTest {

    @Test
    @DisplayName("Verify WorkTemplateStatus lifecycle state transitions")
    void testWorkTemplateStatusTransitions() {
        // Valid transitions
        assertThat(WorkTemplateStatus.DRAFT.canTransitionTo(WorkTemplateStatus.ACTIVE)).isTrue();
        assertThat(WorkTemplateStatus.DRAFT.canTransitionTo(WorkTemplateStatus.ARCHIVED)).isTrue();
        assertThat(WorkTemplateStatus.ACTIVE.canTransitionTo(WorkTemplateStatus.INACTIVE)).isTrue();
        assertThat(WorkTemplateStatus.ACTIVE.canTransitionTo(WorkTemplateStatus.ARCHIVED)).isTrue();
        assertThat(WorkTemplateStatus.INACTIVE.canTransitionTo(WorkTemplateStatus.ACTIVE)).isTrue();
        assertThat(WorkTemplateStatus.INACTIVE.canTransitionTo(WorkTemplateStatus.ARCHIVED)).isTrue();

        assertThatCode(() -> WorkTemplateStatus.DRAFT.validateTransition(WorkTemplateStatus.ACTIVE)).doesNotThrowAnyException();
        assertThatCode(() -> WorkTemplateStatus.ACTIVE.validateTransition(WorkTemplateStatus.INACTIVE)).doesNotThrowAnyException();

        // Invalid transitions
        assertThat(WorkTemplateStatus.DRAFT.canTransitionTo(WorkTemplateStatus.INACTIVE)).isFalse();
        assertThat(WorkTemplateStatus.ARCHIVED.canTransitionTo(WorkTemplateStatus.ACTIVE)).isFalse();

        assertThatThrownBy(() -> WorkTemplateStatus.DRAFT.validateTransition(WorkTemplateStatus.INACTIVE))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Invalid work template status transition from DRAFT to INACTIVE");

        assertThatThrownBy(() -> WorkTemplateStatus.ARCHIVED.validateTransition(WorkTemplateStatus.ACTIVE))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    @DisplayName("Verify WorkInstanceStatus lifecycle state transitions")
    void testWorkInstanceStatusTransitions() {
        // Valid transitions
        assertThat(WorkInstanceStatus.NOT_STARTED.canTransitionTo(WorkInstanceStatus.IN_PROGRESS)).isTrue();
        assertThat(WorkInstanceStatus.NOT_STARTED.canTransitionTo(WorkInstanceStatus.ON_HOLD)).isTrue();
        assertThat(WorkInstanceStatus.NOT_STARTED.canTransitionTo(WorkInstanceStatus.CANCELLED)).isTrue();
        assertThat(WorkInstanceStatus.IN_PROGRESS.canTransitionTo(WorkInstanceStatus.COMPLETED)).isTrue();
        assertThat(WorkInstanceStatus.IN_PROGRESS.canTransitionTo(WorkInstanceStatus.ON_HOLD)).isTrue();
        assertThat(WorkInstanceStatus.IN_PROGRESS.canTransitionTo(WorkInstanceStatus.CANCELLED)).isTrue();
        assertThat(WorkInstanceStatus.ON_HOLD.canTransitionTo(WorkInstanceStatus.IN_PROGRESS)).isTrue();
        assertThat(WorkInstanceStatus.ON_HOLD.canTransitionTo(WorkInstanceStatus.CANCELLED)).isTrue();

        assertThatCode(() -> WorkInstanceStatus.NOT_STARTED.validateTransition(WorkInstanceStatus.IN_PROGRESS)).doesNotThrowAnyException();
        assertThatCode(() -> WorkInstanceStatus.IN_PROGRESS.validateTransition(WorkInstanceStatus.COMPLETED)).doesNotThrowAnyException();

        // Terminal states
        assertThat(WorkInstanceStatus.COMPLETED.canTransitionTo(WorkInstanceStatus.IN_PROGRESS)).isFalse();
        assertThat(WorkInstanceStatus.CANCELLED.canTransitionTo(WorkInstanceStatus.NOT_STARTED)).isFalse();

        assertThatThrownBy(() -> WorkInstanceStatus.COMPLETED.validateTransition(WorkInstanceStatus.IN_PROGRESS))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> WorkInstanceStatus.CANCELLED.validateTransition(WorkInstanceStatus.NOT_STARTED))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    @DisplayName("Verify RecurrenceType period calculations")
    void testRecurrenceCalculations() {
        LocalDate aprilFirst = LocalDate.of(2026, 4, 1);

        // MONTHLY period calculation
        LocalDate monthlyEnd = RecurrenceType.MONTHLY.calculatePeriodEnd(aprilFirst, 1);
        assertThat(monthlyEnd).isEqualTo(LocalDate.of(2026, 4, 30));

        LocalDate nextMonthlyStart = RecurrenceType.MONTHLY.calculateNextPeriodStart(aprilFirst, 1);
        assertThat(nextMonthlyStart).isEqualTo(LocalDate.of(2026, 5, 1));

        // QUARTERLY period calculation (Q1: April - June)
        LocalDate quarterlyEnd = RecurrenceType.QUARTERLY.calculatePeriodEnd(aprilFirst, 1);
        assertThat(quarterlyEnd).isEqualTo(LocalDate.of(2026, 6, 30));

        LocalDate nextQuarterStart = RecurrenceType.QUARTERLY.calculateNextPeriodStart(aprilFirst, 1);
        assertThat(nextQuarterStart).isEqualTo(LocalDate.of(2026, 7, 1));

        // YEARLY period calculation
        LocalDate yearlyEnd = RecurrenceType.YEARLY.calculatePeriodEnd(aprilFirst, 1);
        assertThat(yearlyEnd).isEqualTo(LocalDate.of(2026, 12, 31));

        LocalDate nextYearStart = RecurrenceType.YEARLY.calculateNextPeriodStart(aprilFirst, 1);
        assertThat(nextYearStart).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    @DisplayName("Verify WorkTemplateEntity and WorkTemplateTaskEntity domain models")
    void testWorkTemplateAndTaskModels() {
        UUID templateId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        WorkTemplateEntity template = WorkTemplateEntity.builder()
                .organizationId(orgId)
                .serviceId(serviceId)
                .templateCode("GST_MONTHLY")
                .name("GST Monthly Compliance")
                .description("Monthly GSTR-1, 3B returns")
                .category(ServiceCategory.GST)
                .status(WorkTemplateStatus.ACTIVE)
                .templateType(WorkTemplateType.STATUTORY_COMPLIANCE)
                .recurrenceType(RecurrenceType.MONTHLY)
                .recurrenceInterval(1)
                .recurrenceEnabled(true)
                .isSystemDefault(false)
                .build();
        template.setId(templateId);

        assertThat(template.getId()).isEqualTo(templateId);
        assertThat(template.getOrganizationId()).isEqualTo(orgId);
        assertThat(template.getServiceId()).isEqualTo(serviceId);
        assertThat(template.getTemplateCode()).isEqualTo("GST_MONTHLY");
        assertThat(template.getName()).isEqualTo("GST Monthly Compliance");
        assertThat(template.getCategory()).isEqualTo(ServiceCategory.GST);
        assertThat(template.getStatus()).isEqualTo(WorkTemplateStatus.ACTIVE);
        assertThat(template.getRecurrenceType()).isEqualTo(RecurrenceType.MONTHLY);
        assertThat(template.isRecurrenceEnabled()).isTrue();

        // Template Task
        WorkTemplateTaskEntity task1 = WorkTemplateTaskEntity.builder()
                .templateId(templateId)
                .name("Collect Purchase Register")
                .description("Inward bills and e-way invoices")
                .sequenceOrder(1)
                .defaultAssigneeRole("STAFF")
                .defaultPriority(TaskPriority.HIGH)
                .relativeDueDays(5)
                .mandatory(true)
                .active(true)
                .build();

        assertThat(task1.getTemplateId()).isEqualTo(templateId);
        assertThat(task1.getName()).isEqualTo("Collect Purchase Register");
        assertThat(task1.getSequenceOrder()).isEqualTo(1);
        assertThat(task1.getDefaultAssigneeRole()).isEqualTo("STAFF");
        assertThat(task1.getDefaultPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task1.getRelativeDueDays()).isEqualTo(5);
        assertThat(task1.isMandatory()).isTrue();
        assertThat(task1.isActive()).isTrue();
    }

    @Test
    @DisplayName("Verify EngagementWorkTemplateEntity and WorkInstanceEntity models")
    void testEngagementWorkTemplateAndWorkInstanceModels() {
        UUID orgId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID workInstanceId = UUID.randomUUID();

        // Engagement Work Template
        EngagementWorkTemplateEntity engTemplate = EngagementWorkTemplateEntity.builder()
                .engagementId(engagementId)
                .templateId(templateId)
                .active(true)
                .recurrenceType(RecurrenceType.MONTHLY)
                .recurrenceInterval(1)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2027, 3, 31))
                .build();
        engTemplate.setOrganizationId(orgId);

        assertThat(engTemplate.getOrganizationId()).isEqualTo(orgId);
        assertThat(engTemplate.getEngagementId()).isEqualTo(engagementId);
        assertThat(engTemplate.getTemplateId()).isEqualTo(templateId);
        assertThat(engTemplate.isActive()).isTrue();

        // Work Instance
        WorkInstanceEntity instance = WorkInstanceEntity.builder()
                .engagementId(engagementId)
                .templateId(templateId)
                .title("GST Monthly Compliance — April 2026")
                .periodStart(LocalDate.of(2026, 4, 1))
                .periodEnd(LocalDate.of(2026, 4, 30))
                .dueDate(LocalDate.of(2026, 5, 20))
                .status(WorkInstanceStatus.NOT_STARTED)
                .build();
        instance.setId(workInstanceId);
        instance.setOrganizationId(orgId);

        assertThat(instance.getId()).isEqualTo(workInstanceId);
        assertThat(instance.getOrganizationId()).isEqualTo(orgId);
        assertThat(instance.getEngagementId()).isEqualTo(engagementId);
        assertThat(instance.getTitle()).isEqualTo("GST Monthly Compliance — April 2026");
        assertThat(instance.getPeriodStart()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(instance.getPeriodEnd()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(instance.getDueDate()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(instance.getStatus()).isEqualTo(WorkInstanceStatus.NOT_STARTED);
    }
}
