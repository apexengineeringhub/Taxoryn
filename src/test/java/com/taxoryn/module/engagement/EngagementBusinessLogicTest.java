package com.taxoryn.module.engagement;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.engagement.dto.CreateEngagementRequest;
import com.taxoryn.module.engagement.dto.EngagementDto;
import com.taxoryn.module.engagement.dto.UpdateEngagementAssignmentRequest;
import com.taxoryn.module.engagement.dto.UpdateEngagementStatusRequest;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementPriority;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.service.model.ServiceStatus;
import com.taxoryn.module.service.repository.ServiceRepository;
import com.taxoryn.module.service.service.ServiceCatalogService;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EngagementBusinessLogicTest {

    @Mock
    private EngagementRepository engagementRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ServiceCatalogService serviceCatalogService;

    @Mock
    private ClientServiceRepository clientServiceRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PracticeSecurityScopeEvaluator securityScopeEvaluator;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private com.taxoryn.module.engagement.service.EngagementServiceImpl engagementService;

    private UUID organizationId;
    private UUID clientId;
    private UUID serviceId;
    private UUID assignedUserId;
    private UUID reviewerUserId;
    private ClientEntity clientEntity;
    private ServiceEntity serviceEntity;
    private UserEntity assignedUser;
    private UserEntity reviewerUser;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        TenantContext.setTenantId(organizationId);

        clientId = UUID.randomUUID();
        serviceId = UUID.randomUUID();
        assignedUserId = UUID.randomUUID();
        reviewerUserId = UUID.randomUUID();

        clientEntity = ClientEntity.builder()
                .displayName("Acme Global Pvt Ltd")
                .clientCode("CLI-001")
                .build();
        clientEntity.setId(clientId);
        clientEntity.setOrganizationId(organizationId);

        serviceEntity = ServiceEntity.builder()
                .serviceCode("GST_COMPLIANCE")
                .serviceName("GST Compliance & Returns")
                .category(ServiceCategory.GST)
                .status(ServiceStatus.ACTIVE)
                .moduleCode("GST")
                .build();
        serviceEntity.setId(serviceId);

        assignedUser = UserEntity.builder()
                .email("preparer@firm.com")
                .firstName("Aarav")
                .lastName("Shah")
                .build();
        assignedUser.setId(assignedUserId);
        assignedUser.setOrganizationId(organizationId);

        reviewerUser = UserEntity.builder()
                .email("partner@firm.com")
                .firstName("Rajesh")
                .lastName("Kothari")
                .build();
        reviewerUser.setId(reviewerUserId);
        reviewerUser.setOrganizationId(organizationId);

        PracticeSecurityScope adminScope = PracticeSecurityScope.firmAdmin(UUID.randomUUID());
        when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(adminScope);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Create Engagement generates unique human-readable code and persists successfully")
    void testCreateEngagementSuccess() {
        CreateEngagementRequest request = CreateEngagementRequest.builder()
                .clientId(clientId)
                .serviceId(serviceId)
                .name("Acme GST Compliance FY 2026-27")
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2027, 3, 31))
                .assignedUserId(assignedUserId)
                .reviewerUserId(reviewerUserId)
                .priority(EngagementPriority.HIGH)
                .notes("Monthly retainer")
                .build();

        when(clientRepository.findByIdAndOrganizationId(clientId, organizationId)).thenReturn(Optional.of(clientEntity));
        when(serviceRepository.findAccessibleServiceById(serviceId, organizationId)).thenReturn(Optional.of(serviceEntity));
        when(serviceCatalogService.isServiceAvailableForPractice(organizationId, serviceEntity)).thenReturn(true);
        when(userRepository.findByIdAndOrganizationId(assignedUserId, organizationId)).thenReturn(Optional.of(assignedUser));
        when(userRepository.findByIdAndOrganizationId(reviewerUserId, organizationId)).thenReturn(Optional.of(reviewerUser));
        when(engagementRepository.countByOrganizationId(organizationId)).thenReturn(0L);
        when(engagementRepository.existsByOrganizationIdAndEngagementCode(eq(organizationId), any())).thenReturn(false);

        when(engagementRepository.save(any(EngagementEntity.class))).thenAnswer(invocation -> {
            EngagementEntity e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        EngagementDto result = engagementService.createEngagement(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Acme GST Compliance FY 2026-27");
        assertThat(result.getEngagementCode()).startsWith("ENG-" + LocalDate.now().getYear() + "-");
        assertThat(result.getStatus()).isEqualTo(EngagementStatus.ACTIVE);
        assertThat(result.getPriority()).isEqualTo(EngagementPriority.HIGH);
        assertThat(result.getClientName()).isEqualTo("Acme Global Pvt Ltd");
        assertThat(result.getServiceName()).isEqualTo("GST Compliance & Returns");

        verify(auditService).logEvent(eq("ENGAGEMENT_CREATED"), eq("ENGAGEMENT"), any(), any(), any());
        verify(auditService).logEvent(eq("ENGAGEMENT_ACTIVATED"), eq("ENGAGEMENT"), any(), any(), any());
        verify(auditService).logEvent(eq("ENGAGEMENT_ASSIGNED"), eq("ENGAGEMENT"), any(), any(), any());
    }

    @Test
    @DisplayName("Create Engagement fails when service is not entitled/available for the practice")
    void testCreateEngagementServiceNotAvailable() {
        CreateEngagementRequest request = CreateEngagementRequest.builder()
                .clientId(clientId)
                .serviceId(serviceId)
                .name("Acme GST Compliance")
                .build();

        when(clientRepository.findByIdAndOrganizationId(clientId, organizationId)).thenReturn(Optional.of(clientEntity));
        when(serviceRepository.findAccessibleServiceById(serviceId, organizationId)).thenReturn(Optional.of(serviceEntity));
        when(serviceCatalogService.isServiceAvailableForPractice(organizationId, serviceEntity)).thenReturn(false);

        assertThatThrownBy(() -> engagementService.createEngagement(request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("SERVICE_NOT_AVAILABLE");
    }

    @Test
    @DisplayName("Create Engagement fails when assigned user does not belong to the practice tenant")
    void testCreateEngagementAssignedUserCrossTenant() {
        UUID crossTenantUserId = UUID.randomUUID();
        CreateEngagementRequest request = CreateEngagementRequest.builder()
                .clientId(clientId)
                .serviceId(serviceId)
                .name("Acme GST Compliance")
                .assignedUserId(crossTenantUserId)
                .build();

        when(clientRepository.findByIdAndOrganizationId(clientId, organizationId)).thenReturn(Optional.of(clientEntity));
        when(serviceRepository.findAccessibleServiceById(serviceId, organizationId)).thenReturn(Optional.of(serviceEntity));
        when(serviceCatalogService.isServiceAvailableForPractice(organizationId, serviceEntity)).thenReturn(true);
        when(userRepository.findByIdAndOrganizationId(crossTenantUserId, organizationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> engagementService.createEngagement(request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Assigned preparer/owner does not belong to this organization");
    }

    @Test
    @DisplayName("Create Engagement fails when start date is after end date")
    void testCreateEngagementInvalidDateRange() {
        CreateEngagementRequest request = CreateEngagementRequest.builder()
                .clientId(clientId)
                .name("Invalid Dates")
                .startDate(LocalDate.of(2027, 4, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .build();

        when(clientRepository.findByIdAndOrganizationId(clientId, organizationId)).thenReturn(Optional.of(clientEntity));

        assertThatThrownBy(() -> engagementService.createEngagement(request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Engagement start date cannot be after end date");
    }

    @Test
    @DisplayName("Status transition validation enforces strict lifecycle transitions")
    void testUpdateEngagementStatusLifecycle() {
        UUID engagementId = UUID.randomUUID();
        EngagementEntity engagement = EngagementEntity.builder()
                .clientId(clientId)
                .name("Active Mandate")
                .status(EngagementStatus.ACTIVE)
                .build();
        engagement.setId(engagementId);
        engagement.setOrganizationId(organizationId);

        when(engagementRepository.findByIdAndOrganizationId(engagementId, organizationId)).thenReturn(Optional.of(engagement));
        when(engagementRepository.save(any(EngagementEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACTIVE -> ON_HOLD (Valid)
        UpdateEngagementStatusRequest holdReq = UpdateEngagementStatusRequest.builder()
                .status(EngagementStatus.ON_HOLD)
                .notes("Client data pending")
                .build();
        EngagementDto holdDto = engagementService.updateEngagementStatus(engagementId, holdReq);
        assertThat(holdDto.getStatus()).isEqualTo(EngagementStatus.ON_HOLD);

        // ON_HOLD -> ACTIVE (Valid)
        UpdateEngagementStatusRequest resumeReq = UpdateEngagementStatusRequest.builder()
                .status(EngagementStatus.ACTIVE)
                .notes("Data received, resuming")
                .build();
        EngagementDto resumeDto = engagementService.updateEngagementStatus(engagementId, resumeReq);
        assertThat(resumeDto.getStatus()).isEqualTo(EngagementStatus.ACTIVE);

        // ACTIVE -> COMPLETED (Valid)
        UpdateEngagementStatusRequest completeReq = UpdateEngagementStatusRequest.builder()
                .status(EngagementStatus.COMPLETED)
                .notes("Mandate concluded")
                .build();
        EngagementDto completeDto = engagementService.updateEngagementStatus(engagementId, completeReq);
        assertThat(completeDto.getStatus()).isEqualTo(EngagementStatus.COMPLETED);

        // COMPLETED -> ACTIVE (Invalid - Terminal state)
        UpdateEngagementStatusRequest invalidReq = UpdateEngagementStatusRequest.builder()
                .status(EngagementStatus.ACTIVE)
                .build();
        assertThatThrownBy(() -> engagementService.updateEngagementStatus(engagementId, invalidReq))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Invalid engagement status transition from COMPLETED to ACTIVE");
    }

    @Test
    @DisplayName("Update Engagement Assignment validates users and logs reassignment audit")
    void testUpdateEngagementAssignment() {
        UUID engagementId = UUID.randomUUID();
        EngagementEntity engagement = EngagementEntity.builder()
                .clientId(clientId)
                .name("Assigned Mandate")
                .status(EngagementStatus.ACTIVE)
                .assignedUserId(assignedUserId)
                .build();
        engagement.setId(engagementId);
        engagement.setOrganizationId(organizationId);

        UUID newPreparerId = UUID.randomUUID();
        UserEntity newPreparer = UserEntity.builder()
                .email("newpreparer@firm.com")
                .firstName("Sneha")
                .lastName("Patel")
                .build();
        newPreparer.setId(newPreparerId);
        newPreparer.setOrganizationId(organizationId);

        when(engagementRepository.findByIdAndOrganizationId(engagementId, organizationId)).thenReturn(Optional.of(engagement));
        when(userRepository.findByIdAndOrganizationId(newPreparerId, organizationId)).thenReturn(Optional.of(newPreparer));
        when(engagementRepository.save(any(EngagementEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateEngagementAssignmentRequest assignReq = UpdateEngagementAssignmentRequest.builder()
                .assignedUserId(newPreparerId)
                .notes("Reassigned for FY 26 Q2")
                .build();

        EngagementDto result = engagementService.updateEngagementAssignment(engagementId, assignReq);
        assertThat(result.getAssignedUserId()).isEqualTo(newPreparerId);

        verify(auditService).logEvent(eq("ENGAGEMENT_REASSIGNED"), eq("ENGAGEMENT"), eq(engagementId.toString()), eq(assignedUserId), eq(newPreparerId));
    }
}
