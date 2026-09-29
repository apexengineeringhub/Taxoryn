package com.taxoryn.module.engagement;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementPriority;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.service.model.ServiceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EngagementDomainModelTest {

    @Test
    @DisplayName("Verify EngagementStatus valid and invalid lifecycle state transitions")
    void testEngagementStatusTransitions() {
        // Valid transitions from DRAFT
        assertThat(EngagementStatus.DRAFT.canTransitionTo(EngagementStatus.ACTIVE)).isTrue();
        assertThat(EngagementStatus.DRAFT.canTransitionTo(EngagementStatus.CANCELLED)).isTrue();
        assertThat(EngagementStatus.DRAFT.canTransitionTo(EngagementStatus.DRAFT)).isTrue();
        assertThatCode(() -> EngagementStatus.DRAFT.validateTransition(EngagementStatus.ACTIVE)).doesNotThrowAnyException();
        assertThatCode(() -> EngagementStatus.DRAFT.validateTransition(EngagementStatus.CANCELLED)).doesNotThrowAnyException();

        // Invalid transitions from DRAFT
        assertThat(EngagementStatus.DRAFT.canTransitionTo(EngagementStatus.COMPLETED)).isFalse();
        assertThat(EngagementStatus.DRAFT.canTransitionTo(EngagementStatus.ON_HOLD)).isFalse();
        assertThatThrownBy(() -> EngagementStatus.DRAFT.validateTransition(EngagementStatus.COMPLETED))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Invalid engagement status transition from DRAFT to COMPLETED");
        assertThatThrownBy(() -> EngagementStatus.DRAFT.validateTransition(EngagementStatus.ON_HOLD))
                .isInstanceOf(BusinessValidationException.class);

        // Valid transitions from ACTIVE
        assertThat(EngagementStatus.ACTIVE.canTransitionTo(EngagementStatus.ON_HOLD)).isTrue();
        assertThat(EngagementStatus.ACTIVE.canTransitionTo(EngagementStatus.COMPLETED)).isTrue();
        assertThat(EngagementStatus.ACTIVE.canTransitionTo(EngagementStatus.CANCELLED)).isTrue();
        assertThatCode(() -> EngagementStatus.ACTIVE.validateTransition(EngagementStatus.ON_HOLD)).doesNotThrowAnyException();
        assertThatCode(() -> EngagementStatus.ACTIVE.validateTransition(EngagementStatus.COMPLETED)).doesNotThrowAnyException();
        assertThatCode(() -> EngagementStatus.ACTIVE.validateTransition(EngagementStatus.CANCELLED)).doesNotThrowAnyException();

        // Invalid transition from ACTIVE
        assertThat(EngagementStatus.ACTIVE.canTransitionTo(EngagementStatus.DRAFT)).isFalse();
        assertThatThrownBy(() -> EngagementStatus.ACTIVE.validateTransition(EngagementStatus.DRAFT))
                .isInstanceOf(BusinessValidationException.class);

        // Valid transitions from ON_HOLD
        assertThat(EngagementStatus.ON_HOLD.canTransitionTo(EngagementStatus.ACTIVE)).isTrue();
        assertThat(EngagementStatus.ON_HOLD.canTransitionTo(EngagementStatus.CANCELLED)).isTrue();
        assertThatCode(() -> EngagementStatus.ON_HOLD.validateTransition(EngagementStatus.ACTIVE)).doesNotThrowAnyException();
        assertThatCode(() -> EngagementStatus.ON_HOLD.validateTransition(EngagementStatus.CANCELLED)).doesNotThrowAnyException();

        // Invalid transitions from ON_HOLD
        assertThat(EngagementStatus.ON_HOLD.canTransitionTo(EngagementStatus.COMPLETED)).isFalse();
        assertThat(EngagementStatus.ON_HOLD.canTransitionTo(EngagementStatus.DRAFT)).isFalse();
        assertThatThrownBy(() -> EngagementStatus.ON_HOLD.validateTransition(EngagementStatus.COMPLETED))
                .isInstanceOf(BusinessValidationException.class);

        // Terminal states: COMPLETED and CANCELLED cannot transition anywhere
        for (EngagementStatus target : EngagementStatus.values()) {
            if (target != EngagementStatus.COMPLETED) {
                assertThat(EngagementStatus.COMPLETED.canTransitionTo(target)).isFalse();
                assertThatThrownBy(() -> EngagementStatus.COMPLETED.validateTransition(target))
                        .isInstanceOf(BusinessValidationException.class);
            }
            if (target != EngagementStatus.CANCELLED) {
                assertThat(EngagementStatus.CANCELLED.canTransitionTo(target)).isFalse();
                assertThatThrownBy(() -> EngagementStatus.CANCELLED.validateTransition(target))
                        .isInstanceOf(BusinessValidationException.class);
            }
        }
    }

    @Test
    @DisplayName("Verify EngagementPriority enum values")
    void testEngagementPriorityEnum() {
        assertThat(EngagementPriority.values()).containsExactly(
                EngagementPriority.LOW,
                EngagementPriority.MEDIUM,
                EngagementPriority.HIGH,
                EngagementPriority.URGENT
        );
    }

    @Test
    @DisplayName("Verify ServiceCategory and ServiceStatus enum values")
    void testServiceEnums() {
        assertThat(ServiceCategory.values()).containsExactly(
                ServiceCategory.GST,
                ServiceCategory.TDS,
                ServiceCategory.ITR,
                ServiceCategory.AUDIT,
                ServiceCategory.NOTICE,
                ServiceCategory.ADVISORY,
                ServiceCategory.OTHER
        );

        assertThat(ServiceStatus.values()).containsExactly(
                ServiceStatus.ACTIVE,
                ServiceStatus.INACTIVE,
                ServiceStatus.ARCHIVED
        );
    }

    @Test
    @DisplayName("Verify ServiceEntity domain model attributes")
    void testServiceEntityModel() {
        UUID serviceId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        ServiceEntity service = ServiceEntity.builder()
                .organizationId(orgId)
                .serviceCode("GST_COMPLIANCE")
                .serviceName("GST Compliance & Returns")
                .description("Monthly GSTR-1, 3B returns")
                .category(ServiceCategory.GST)
                .status(ServiceStatus.ACTIVE)
                .configurable(true)
                .moduleCode("GST")
                .build();
        service.setId(serviceId);

        assertThat(service.getId()).isEqualTo(serviceId);
        assertThat(service.getOrganizationId()).isEqualTo(orgId);
        assertThat(service.getServiceCode()).isEqualTo("GST_COMPLIANCE");
        assertThat(service.getServiceName()).isEqualTo("GST Compliance & Returns");
        assertThat(service.getDescription()).isEqualTo("Monthly GSTR-1, 3B returns");
        assertThat(service.getCategory()).isEqualTo(ServiceCategory.GST);
        assertThat(service.getStatus()).isEqualTo(ServiceStatus.ACTIVE);
        assertThat(service.isConfigurable()).isTrue();
        assertThat(service.getModuleCode()).isEqualTo("GST");
    }

    @Test
    @DisplayName("Verify EngagementEntity domain model attributes")
    void testEngagementEntityModel() {
        UUID engagementId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID assignedUserId = UUID.randomUUID();
        UUID reviewerUserId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        EngagementEntity engagement = EngagementEntity.builder()
                .clientId(clientId)
                .serviceId(serviceId)
                .locationId(locationId)
                .engagementCode("ENG-2026-000001")
                .name("ABC Pvt Ltd - GST Compliance FY 2026-27")
                .description("Annual GST compliance mandate")
                .status(EngagementStatus.DRAFT)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2027, 3, 31))
                .assignedUserId(assignedUserId)
                .reviewerUserId(reviewerUserId)
                .priority(EngagementPriority.HIGH)
                .notes("Key client mandate")
                .build();
        engagement.setId(engagementId);
        engagement.setOrganizationId(orgId);

        assertThat(engagement.getId()).isEqualTo(engagementId);
        assertThat(engagement.getOrganizationId()).isEqualTo(orgId);
        assertThat(engagement.getClientId()).isEqualTo(clientId);
        assertThat(engagement.getServiceId()).isEqualTo(serviceId);
        assertThat(engagement.getLocationId()).isEqualTo(locationId);
        assertThat(engagement.getEngagementCode()).isEqualTo("ENG-2026-000001");
        assertThat(engagement.getName()).isEqualTo("ABC Pvt Ltd - GST Compliance FY 2026-27");
        assertThat(engagement.getStatus()).isEqualTo(EngagementStatus.DRAFT);
        assertThat(engagement.getStartDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(engagement.getEndDate()).isEqualTo(LocalDate.of(2027, 3, 31));
        assertThat(engagement.getAssignedUserId()).isEqualTo(assignedUserId);
        assertThat(engagement.getReviewerUserId()).isEqualTo(reviewerUserId);
        assertThat(engagement.getPriority()).isEqualTo(EngagementPriority.HIGH);
        assertThat(engagement.getNotes()).isEqualTo("Key client mandate");
    }
}
