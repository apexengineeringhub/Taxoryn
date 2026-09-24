package com.taxoryn.module.compliance.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientServiceEntity;
import com.taxoryn.module.client.entity.ClientServiceStatus;
import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.compliance.dto.AssignComplianceWorkRequest;
import com.taxoryn.module.compliance.dto.ComplianceWorkItemDto;
import com.taxoryn.module.compliance.dto.CreateComplianceWorkItemRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceWorkItemRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceWorkStatusRequest;
import com.taxoryn.module.compliance.entity.ComplianceWorkItemEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkStatus;
import com.taxoryn.module.compliance.entity.ComplianceWorkType;
import com.taxoryn.module.compliance.repository.ComplianceWorkItemRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.moduleconfig.dto.OrganizationModuleDto;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceWorkItemServiceTest {

    @Mock
    private ComplianceWorkItemRepository workItemRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ClientServiceRepository clientServiceRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ModuleConfigurationService moduleConfigurationService;
    @Mock
    private AuditService auditService;
    @Mock
    private PracticeSecurityScopeEvaluator securityScopeEvaluator;

    @InjectMocks
    private ComplianceWorkItemServiceImpl workItemService;

    private MockedStatic<SecurityUtils> mockedSecurityUtils;

    private UUID organizationId;
    private UUID userId;
    private UUID clientId;
    private UUID clientServiceId;
    private UUID workItemId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        clientServiceId = UUID.randomUUID();
        workItemId = UUID.randomUUID();
        employeeId = UUID.randomUUID();

        mockedSecurityUtils = mockStatic(SecurityUtils.class);
        mockedSecurityUtils.when(SecurityUtils::getCurrentOrganizationId).thenReturn(organizationId);
        mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

        OrganizationModuleDto gstMod = OrganizationModuleDto.builder()
                .moduleCode(ProductModuleCode.GST)
                .enabled(true)
                .entitled(true)
                .build();
        OrganizationModuleDto itrMod = OrganizationModuleDto.builder()
                .moduleCode(ProductModuleCode.ITR)
                .enabled(true)
                .entitled(true)
                .build();
        OrganizationModuleDto tdsMod = OrganizationModuleDto.builder()
                .moduleCode(ProductModuleCode.TDS)
                .enabled(true)
                .entitled(true)
                .build();

        org.mockito.Mockito.lenient().when(moduleConfigurationService.getOrganizationModules(organizationId))
                .thenReturn(List.of(gstMod, itrMod, tdsMod));

        org.mockito.Mockito.lenient().when(securityScopeEvaluator.evaluateCurrentScope())
                .thenReturn(PracticeSecurityScope.builder().isFirmAdmin(true).build());
        org.mockito.Mockito.lenient().when(securityScopeEvaluator.getAccessibleClientIds(any()))
                .thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtils.close();
    }

    @Test
    @DisplayName("Should create compliance work item successfully")
    void testCreateComplianceWorkItemSuccess() {
        ClientEntity client = ClientEntity.builder().displayName("Apex Corp").build();
        client.setId(clientId);
        client.setOrganizationId(organizationId);

        ClientServiceEntity service = ClientServiceEntity.builder()
                .clientId(clientId)
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .status(ClientServiceStatus.ACTIVE)
                .build();
        service.setId(clientServiceId);
        service.setOrganizationId(organizationId);

        when(clientRepository.findByIdAndOrganizationId(clientId, organizationId)).thenReturn(Optional.of(client));
        when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(PracticeSecurityScope.builder().isFirmAdmin(true).build());
        when(securityScopeEvaluator.getAccessibleClientIds(any())).thenReturn(null);
        when(clientServiceRepository.findByIdAndOrganizationIdAndClientId(clientServiceId, organizationId, clientId)).thenReturn(Optional.of(service));

        ComplianceWorkItemEntity savedEntity = ComplianceWorkItemEntity.builder()
                .clientId(clientId)
                .clientServiceId(clientServiceId)
                .workType(ComplianceWorkType.GST_RETURN)
                .title("GSTR-3B September 2026")
                .status(ComplianceWorkStatus.NOT_STARTED)
                .statutoryDueDate(LocalDate.of(2026, 10, 20))
                .internalTargetDate(LocalDate.of(2026, 10, 15))
                .build();
        savedEntity.setId(workItemId);
        savedEntity.setOrganizationId(organizationId);

        when(workItemRepository.save(any(ComplianceWorkItemEntity.class))).thenReturn(savedEntity);
        when(employeeRepository.findAllByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

        CreateComplianceWorkItemRequest request = CreateComplianceWorkItemRequest.builder()
                .clientId(clientId)
                .clientServiceId(clientServiceId)
                .workType(ComplianceWorkType.GST_RETURN)
                .title("GSTR-3B September 2026")
                .statutoryDueDate(LocalDate.of(2026, 10, 20))
                .internalTargetDate(LocalDate.of(2026, 10, 15))
                .build();

        ComplianceWorkItemDto result = workItemService.createComplianceWorkItem(request);

        assertNotNull(result);
        assertEquals("GSTR-3B September 2026", result.getTitle());
        assertEquals(ComplianceWorkStatus.NOT_STARTED, result.getStatus());
        verify(auditService).logEvent(eq(organizationId), eq(userId), eq("COMPLIANCE_WORK_CREATED"), eq("COMPLIANCE_WORK_ITEM"), eq(workItemId.toString()), any(), any());
    }

    @Test
    @DisplayName("Should reject creation when internal target date is after statutory due date")
    void testCreateWorkItemInvalidDates() {
        ClientEntity client = ClientEntity.builder().displayName("Apex Corp").build();
        client.setId(clientId);
        client.setOrganizationId(organizationId);

        ClientServiceEntity service = ClientServiceEntity.builder()
                .clientId(clientId)
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .build();
        service.setId(clientServiceId);

        when(clientRepository.findByIdAndOrganizationId(clientId, organizationId)).thenReturn(Optional.of(client));
        when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(PracticeSecurityScope.builder().isFirmAdmin(true).build());
        when(clientServiceRepository.findByIdAndOrganizationIdAndClientId(clientServiceId, organizationId, clientId)).thenReturn(Optional.of(service));

        CreateComplianceWorkItemRequest request = CreateComplianceWorkItemRequest.builder()
                .clientId(clientId)
                .clientServiceId(clientServiceId)
                .workType(ComplianceWorkType.GST_RETURN)
                .title("GSTR-3B September 2026")
                .statutoryDueDate(LocalDate.of(2026, 10, 20))
                .internalTargetDate(LocalDate.of(2026, 10, 25)) // Target after due date
                .build();

        assertThrows(BusinessValidationException.class, () -> workItemService.createComplianceWorkItem(request));
    }

    @Test
    @DisplayName("Should validate valid status transitions and reject invalid ones")
    void testStatusTransitions() {
        ComplianceWorkItemEntity item = ComplianceWorkItemEntity.builder()
                .clientId(clientId)
                .clientServiceId(clientServiceId)
                .workType(ComplianceWorkType.GST_RETURN)
                .title("GSTR-1 September 2026")
                .status(ComplianceWorkStatus.NOT_STARTED)
                .build();
        item.setId(workItemId);
        item.setOrganizationId(organizationId);

        when(workItemRepository.findByIdAndOrganizationId(workItemId, organizationId)).thenReturn(Optional.of(item));
        when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(PracticeSecurityScope.builder().isFirmAdmin(true).build());
        when(securityScopeEvaluator.getAccessibleClientIds(any())).thenReturn(null);
        when(workItemRepository.save(any(ComplianceWorkItemEntity.class))).thenReturn(item);

        // 1. Valid Transition: NOT_STARTED -> IN_PREPARATION
        UpdateComplianceWorkStatusRequest validReq = UpdateComplianceWorkStatusRequest.builder()
                .status(ComplianceWorkStatus.IN_PREPARATION)
                .notes("Started work")
                .build();
        ComplianceWorkItemDto updated = workItemService.updateComplianceWorkStatus(workItemId, validReq);
        assertEquals(ComplianceWorkStatus.IN_PREPARATION, updated.getStatus());

        // 2. Invalid Transition: COMPLETED cannot transition to IN_PREPARATION
        item.setStatus(ComplianceWorkStatus.COMPLETED);
        UpdateComplianceWorkStatusRequest invalidReq = UpdateComplianceWorkStatusRequest.builder()
                .status(ComplianceWorkStatus.IN_PREPARATION)
                .build();

        assertThrows(BusinessValidationException.class, () -> workItemService.updateComplianceWorkStatus(workItemId, invalidReq));
    }

    @Test
    @DisplayName("Should enforce client portfolio scope on work item lookup")
    void testClientPortfolioScopeEnforcement() {
        ComplianceWorkItemEntity item = ComplianceWorkItemEntity.builder()
                .clientId(clientId)
                .clientServiceId(clientServiceId)
                .workType(ComplianceWorkType.ITR_RETURN)
                .title("ITR-6 Filing")
                .status(ComplianceWorkStatus.IN_PREPARATION)
                .build();
        item.setId(workItemId);
        item.setOrganizationId(organizationId);

        when(workItemRepository.findByIdAndOrganizationId(workItemId, organizationId)).thenReturn(Optional.of(item));

        // Caller has scope restricted to another client
        PracticeSecurityScope restrictedScope = PracticeSecurityScope.builder()
                .isFirmAdmin(false)
                .employeeId(UUID.randomUUID())
                .userId(userId)
                .build();
        when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(restrictedScope);
        when(securityScopeEvaluator.getAccessibleClientIds(restrictedScope)).thenReturn(Set.of(UUID.randomUUID())); // Does not contain clientId

        assertThrows(ForbiddenException.class, () -> workItemService.getComplianceWorkItemById(workItemId));
    }
}
