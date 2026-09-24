package com.taxoryn.qa.security;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.dto.AssignClientEmployeeRequest;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.ClientFilterRequest;
import com.taxoryn.module.client.dto.CreateClientNoteRequest;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientStatusRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientNoteEntity;
import com.taxoryn.module.client.mapper.ClientMapper;
import com.taxoryn.module.client.repository.ClientNoteRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.service.ClientServiceImpl;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.subscription.service.SubscriptionService;
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.mapper.TaskMapper;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.service.TaskServiceImpl;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientAccessScopeSecurityIntegrationTest {

    private final UUID orgA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID orgB = UUID.fromString("22222222-2222-2222-2222-222222222222");

    // Organization A Users & Employees
    private final UUID ownerAUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-111111111111");
    private final UUID managerAUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-222222222222");
    private final UUID managerAEmpId = UUID.fromString("aaaaaaaa-0000-0000-0000-222222222223");

    private final UUID practitionerA1UserId = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private final UUID practitionerA1EmpId = UUID.fromString("aaaaaaaa-1111-1111-1111-222222222222");

    private final UUID practitionerA2UserId = UUID.fromString("aaaaaaaa-2222-2222-2222-111111111111");
    private final UUID practitionerA2EmpId = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");

    private final UUID staffA1UserId = UUID.fromString("aaaaaaaa-3333-3333-3333-111111111111");
    private final UUID staffA1EmpId = UUID.fromString("aaaaaaaa-3333-3333-3333-222222222222");

    private final UUID clientPortalUserId = UUID.fromString("aaaaaaaa-4444-4444-4444-111111111111");

    // Organization A Clients
    private final UUID clientA1Id = UUID.fromString("aaaaaaaa-1111-1111-1111-333333333333"); // Assigned to Practitioner A1 (Direct Tax)
    private final UUID clientA2Id = UUID.fromString("aaaaaaaa-2222-2222-2222-333333333333"); // Assigned to Practitioner A2 (Audit)
    private final UUID clientA3Id = UUID.fromString("aaaaaaaa-3333-3333-3333-333333333333"); // Assigned to Staff A1 (Direct Tax)
    private final UUID clientAUnassignedId = UUID.fromString("aaaaaaaa-9999-9999-9999-333333333333"); // Unassigned

    // Organization B Users, Employees, Clients
    private final UUID userB1Id = UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111");
    private final UUID empB1Id = UUID.fromString("bbbbbbbb-1111-1111-1111-222222222222");
    private final UUID clientB1Id = UUID.fromString("bbbbbbbb-1111-1111-1111-333333333333");

    @Mock private ClientRepository clientRepository;
    @Mock private ClientNoteRepository clientNoteRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaxNoticeRepository noticeRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private AuditService auditService;
    @Mock private ClientMapper clientMapper;
    @Mock private TaskMapper taskMapper;

    private PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private ClientServiceImpl clientService;
    private TaskServiceImpl taskService;

    private EmployeeEntity empManagerA;
    private EmployeeEntity empPractitionerA1;
    private EmployeeEntity empPractitionerA2;
    private EmployeeEntity empStaffA1;
    private EmployeeEntity empB1;

    private ClientEntity clientA1;
    private ClientEntity clientA2;
    private ClientEntity clientA3;
    private ClientEntity clientAUnassigned;
    private ClientEntity clientB1;

    @BeforeEach
    void setUp() {
        securityScopeEvaluator = new PracticeSecurityScopeEvaluator(employeeRepository, clientRepository, taskRepository);

        clientService = new ClientServiceImpl(
                clientRepository,
                clientNoteRepository,
                employeeRepository,
                taskRepository,
                userRepository,
                mock(com.taxoryn.module.role.repository.RoleRepository.class),
                mock(com.taxoryn.module.organization.repository.OrganizationRepository.class),
                mock(com.taxoryn.module.authentication.repository.OrganizationActivationTokenRepository.class),
                mock(com.taxoryn.module.authentication.repository.RefreshTokenRepository.class),
                mock(com.taxoryn.module.notification.email.service.EmailNotificationService.class),
                subscriptionService,
                securityScopeEvaluator,
                noticeRepository,
                mock(com.taxoryn.module.gst.repository.GstProfileRepository.class),
                mock(com.taxoryn.module.gst.repository.GstReturnFilingRepository.class),
                mock(com.taxoryn.module.itr.repository.ItrProfileRepository.class),
                mock(com.taxoryn.module.itr.repository.ItrReturnRepository.class),
                mock(com.taxoryn.module.tds.repository.TdsProfileRepository.class),
                mock(com.taxoryn.module.tds.repository.TdsReturnRepository.class),
                mock(com.taxoryn.module.document.repository.DocumentRepository.class),
                mock(com.taxoryn.module.docrequest.repository.DocumentRequestRepository.class),
                mock(com.taxoryn.module.billing.repository.InvoiceRepository.class),
                mock(com.taxoryn.module.audit.repository.AuditLogRepository.class),
                mock(com.taxoryn.module.client.repository.ClientServiceRepository.class),
                clientMapper,
                taskMapper,
                auditService
        );

        taskService = new TaskServiceImpl(
                taskRepository,
                clientRepository,
                employeeRepository,
                userRepository,
                mock(com.taxoryn.module.compliance.repository.ComplianceObligationRepository.class),
                mock(com.taxoryn.module.docrequest.repository.DocumentRequestRepository.class),
                mock(com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository.class),
                securityScopeEvaluator,
                taskMapper,
                mock(com.taxoryn.module.notification.service.NotificationService.class)
        );

        // Build Employees
        empManagerA = EmployeeEntity.builder()
                .userId(managerAUserId)
                .employeeCode("EMP-MGR-A")
                .firstName("Tax")
                .lastName("Manager")
                .email("mgr@orga.com")
                .department("Direct Tax")
                .build();
        empManagerA.setId(managerAEmpId);
        empManagerA.setOrganizationId(orgA);

        empPractitionerA1 = EmployeeEntity.builder()
                .userId(practitionerA1UserId)
                .employeeCode("EMP-PRAC-A1")
                .firstName("Practitioner")
                .lastName("A1")
                .email("prac1@orga.com")
                .department("Direct Tax")
                .managerId(managerAEmpId)
                .build();
        empPractitionerA1.setId(practitionerA1EmpId);
        empPractitionerA1.setOrganizationId(orgA);

        empPractitionerA2 = EmployeeEntity.builder()
                .userId(practitionerA2UserId)
                .employeeCode("EMP-PRAC-A2")
                .firstName("Practitioner")
                .lastName("A2")
                .email("prac2@orga.com")
                .department("Audit")
                .build();
        empPractitionerA2.setId(practitionerA2EmpId);
        empPractitionerA2.setOrganizationId(orgA);

        empStaffA1 = EmployeeEntity.builder()
                .userId(staffA1UserId)
                .employeeCode("EMP-STAFF-A1")
                .firstName("Staff")
                .lastName("A1")
                .email("staff1@orga.com")
                .department("Direct Tax")
                .managerId(practitionerA1EmpId) // Practitioner A1 is supervisor/line manager of Staff A1
                .build();
        empStaffA1.setId(staffA1EmpId);
        empStaffA1.setOrganizationId(orgA);

        empB1 = EmployeeEntity.builder()
                .userId(userB1Id)
                .employeeCode("EMP-B1")
                .firstName("Practitioner")
                .lastName("B1")
                .email("b1@orgb.com")
                .department("Tax")
                .build();
        empB1.setId(empB1Id);
        empB1.setOrganizationId(orgB);

        // Build Clients
        clientA1 = ClientEntity.builder()
                .displayName("Client A1 Direct Tax")
                .pan("ABCDE1234F")
                .assignedEmployeeId(practitionerA1EmpId)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        clientA1.setId(clientA1Id);
        clientA1.setOrganizationId(orgA);

        clientA2 = ClientEntity.builder()
                .displayName("Client A2 Audit")
                .pan("FGHIJ5678K")
                .assignedEmployeeId(practitionerA2EmpId)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        clientA2.setId(clientA2Id);
        clientA2.setOrganizationId(orgA);

        clientA3 = ClientEntity.builder()
                .displayName("Client A3 Staff Direct Tax")
                .pan("UVWXY1234Z")
                .assignedEmployeeId(staffA1EmpId)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        clientA3.setId(clientA3Id);
        clientA3.setOrganizationId(orgA);

        clientAUnassigned = ClientEntity.builder()
                .displayName("Client A Unassigned")
                .pan("UNASN1234U")
                .assignedEmployeeId(null)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        clientAUnassigned.setId(clientAUnassignedId);
        clientAUnassigned.setOrganizationId(orgA);

        clientB1 = ClientEntity.builder()
                .displayName("Client B1 Tenant B")
                .pan("KLMNO9012P")
                .assignedEmployeeId(empB1Id)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        clientB1.setId(clientB1Id);
        clientB1.setOrganizationId(orgB);

        // Default DTO Mapper mock
        when(clientMapper.toDto(any(ClientEntity.class))).thenAnswer(inv -> {
            ClientEntity c = inv.getArgument(0);
            return ClientDto.builder()
                    .id(c.getId())
                    .displayName(c.getDisplayName())
                    .pan(c.getPan())
                    .assignedEmployeeId(c.getAssignedEmployeeId())
                    .status(c.getStatus())
                    .build();
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void authenticateAs(UUID userId, UUID orgId, String email, Set<String> roles) {
        authenticateAs(userId, orgId, email, roles, null);
    }

    private void authenticateAs(UUID userId, UUID orgId, String email, Set<String> roles, UUID clientId) {
        SecurityUser.SecurityUserBuilder builder = SecurityUser.builder()
                .userId(userId)
                .organizationId(orgId)
                .email(email)
                .roles(roles)
                .permissions(Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE", "TASK_CREATE", "TASK_VIEW", "TASK_UPDATE"))
                .enabled(true);

        if (clientId != null) {
            builder.clientId(clientId);
        }

        SecurityUser principal = builder.build();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(orgId);
    }

    // =========================================================================
    // 1. HARD MULTI-TENANT ISOLATION & DIRECT-ID ATTACK TESTS
    // =========================================================================
    @Nested
    @DisplayName("1. Multi-Tenant Isolation & Direct-ID Protection")
    class TenantIsolationTests {

        @Test
        @DisplayName("Owner/Admin in Org A attempting to access Client in Org B receives 404 ResourceNotFoundException")
        void testCrossTenantDirectIdLookupFails() {
            authenticateAs(ownerAUserId, orgA, "owner@orga.com", Set.of("ORG_ADMIN", "ROLE_ORG_ADMIN"));

            when(clientRepository.findByIdAndOrganizationId(clientB1Id, orgA)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> clientService.getClientById(clientB1Id));
        }

        @Test
        @DisplayName("Assigning an employee from another tenant (Org B) to Client in Org A is rejected")
        void testCrossTenantEmployeeAssignmentRejected() {
            authenticateAs(ownerAUserId, orgA, "owner@orga.com", Set.of("ORG_ADMIN", "ROLE_ORG_ADMIN"));

            when(clientRepository.findByIdAndOrganizationId(clientA1Id, orgA)).thenReturn(Optional.of(clientA1));
            when(employeeRepository.findByIdAndOrganizationId(empB1Id, orgA)).thenReturn(Optional.empty());

            AssignClientEmployeeRequest req = new AssignClientEmployeeRequest(empB1Id);
            assertThrows(ResourceNotFoundException.class, () -> clientService.assignEmployee(clientA1Id, req));
        }
    }

    // =========================================================================
    // 2. OWNER & ORG_ADMIN SCOPE TESTS
    // =========================================================================
    @Nested
    @DisplayName("2. Owner & Org Admin Organization-Wide Scope")
    class OwnerOrgAdminScopeTests {

        @Test
        @DisplayName("ORG_ADMIN has unrestricted organization-wide access to all clients in their tenant")
        void testOrgAdminHasUnrestrictedAccess() {
            authenticateAs(ownerAUserId, orgA, "owner@orga.com", Set.of("ORG_ADMIN", "ROLE_ORG_ADMIN"));

            when(clientRepository.findByIdAndOrganizationId(clientA1Id, orgA)).thenReturn(Optional.of(clientA1));
            when(clientRepository.findByIdAndOrganizationId(clientA2Id, orgA)).thenReturn(Optional.of(clientA2));
            when(clientRepository.findByIdAndOrganizationId(clientAUnassignedId, orgA)).thenReturn(Optional.of(clientAUnassigned));

            PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
            assertTrue(scope.isFirmAdmin());
            assertNull(scope.getAccessibleAssigneeIds());
            assertNull(securityScopeEvaluator.getAccessibleClientIds(scope));

            assertNotNull(clientService.getClientById(clientA1Id));
            assertNotNull(clientService.getClientById(clientA2Id));
            assertNotNull(clientService.getClientById(clientAUnassignedId));
        }

        @Test
        @DisplayName("PRACTICE_OWNER and PARTNER have full firm-wide client access")
        void testPartnerAndPracticeOwnerScope() {
            authenticateAs(ownerAUserId, orgA, "owner@orga.com", Set.of("PRACTICE_OWNER", "ROLE_PRACTICE_OWNER", "ROLE_PARTNER"));

            PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
            assertTrue(scope.isFirmAdmin());
            assertNull(scope.getAccessibleAssigneeIds());
        }
    }

    // =========================================================================
    // 3. MANAGER SCOPE & REPORTING HIERARCHY SEPARATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("3. Manager Scope vs Reporting Hierarchy Non-Promotion")
    class ManagerScopeTests {

        @Test
        @DisplayName("Explicit ROLE_MANAGER grants team/department scope (Direct Tax clients)")
        void testExplicitRoleManagerGrantsDepartmentScope() {
            authenticateAs(managerAUserId, orgA, "mgr@orga.com", Set.of("MANAGER", "ROLE_MANAGER"));

            when(employeeRepository.findByOrganizationIdAndUserId(orgA, managerAUserId)).thenReturn(Optional.of(empManagerA));
            when(employeeRepository.findAllByOrganizationId(orgA)).thenReturn(List.of(empManagerA, empPractitionerA1, empStaffA1, empPractitionerA2));

            // In Direct Tax department: empManagerA, empPractitionerA1, empStaffA1
            when(clientRepository.findIdsByOrganizationIdAndAssignedEmployeeIdIn(eq(orgA), any()))
                    .thenReturn(List.of(clientA1Id, clientA3Id));

            when(clientRepository.findByIdAndOrganizationId(clientA1Id, orgA)).thenReturn(Optional.of(clientA1));
            when(clientRepository.findByIdAndOrganizationId(clientA2Id, orgA)).thenReturn(Optional.of(clientA2));

            PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
            assertEquals(PracticeSecurityScope.RoleTier.DEPARTMENT_MANAGER, scope.getRoleTier());
            assertTrue(scope.isDepartmentManager());
            assertFalse(scope.isFirmAdmin());

            // Department Direct Tax includes Manager, Prac A1, Staff A1
            assertTrue(scope.getAccessibleAssigneeIds().contains(managerAEmpId));
            assertTrue(scope.getAccessibleAssigneeIds().contains(practitionerA1EmpId));
            assertTrue(scope.getAccessibleAssigneeIds().contains(staffA1EmpId));
            // Does NOT include Prac A2 (Audit department)
            assertFalse(scope.getAccessibleAssigneeIds().contains(practitionerA2EmpId));

            // Can access Client A1 (assigned to Direct Tax practitioner)
            assertNotNull(clientService.getClientById(clientA1Id));

            // CANNOT access Client A2 (assigned to Audit practitioner)
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientA2Id));
        }

        @Test
        @DisplayName("INVARIANT: Practitioner having subordinate reportees does NOT gain Manager scope without explicit MANAGER role")
        void testPractitionerWithReporteesRemainsStaffIndividualScoped() {
            // Practitioner A1 is supervisor of Staff A1 in employee hierarchy, but has only ROLE_PRACTITIONER
            authenticateAs(practitionerA1UserId, orgA, "prac1@orga.com", Set.of("PRACTITIONER", "ROLE_PRACTITIONER"));

            when(employeeRepository.findByOrganizationIdAndUserId(orgA, practitionerA1UserId)).thenReturn(Optional.of(empPractitionerA1));
            when(clientRepository.findIdsByOrganizationIdAndAssignedEmployeeIdIn(eq(orgA), any()))
                    .thenReturn(List.of(clientA1Id));

            when(clientRepository.findByIdAndOrganizationId(clientA1Id, orgA)).thenReturn(Optional.of(clientA1));
            when(clientRepository.findByIdAndOrganizationId(clientA3Id, orgA)).thenReturn(Optional.of(clientA3));

            PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

            assertEquals(PracticeSecurityScope.RoleTier.STAFF_INDIVIDUAL, scope.getRoleTier());
            assertFalse(scope.isDepartmentManager());
            assertFalse(scope.isFirmAdmin());
            assertTrue(scope.isStaff());

            // Accessible assignee IDs contain only self (Practitioner A1)
            assertTrue(scope.getAccessibleAssigneeIds().contains(practitionerA1UserId));
            assertTrue(scope.getAccessibleAssigneeIds().contains(practitionerA1EmpId));
            assertFalse(scope.getAccessibleAssigneeIds().contains(staffA1EmpId));

            // Can access Client A1 (assigned to self)
            assertNotNull(clientService.getClientById(clientA1Id));

            // CANNOT access Client A3 (assigned to reportee Staff A1)
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientA3Id));
        }
    }

    // =========================================================================
    // 4. PRACTITIONER & STAFF INDIVIDUAL SCOPE TESTS
    // =========================================================================
    @Nested
    @DisplayName("4. Practitioner & Staff Scope Restrictions")
    class PractitionerAndStaffScopeTests {

        @Test
        @DisplayName("Practitioner A1 cannot access Client A2 (assigned to Practitioner A2) or unassigned clients")
        void testPractitionerIsolation() {
            authenticateAs(practitionerA1UserId, orgA, "prac1@orga.com", Set.of("PRACTITIONER", "ROLE_PRACTITIONER"));

            when(employeeRepository.findByOrganizationIdAndUserId(orgA, practitionerA1UserId)).thenReturn(Optional.of(empPractitionerA1));
            when(clientRepository.findIdsByOrganizationIdAndAssignedEmployeeIdIn(eq(orgA), any()))
                    .thenReturn(List.of(clientA1Id));

            when(clientRepository.findByIdAndOrganizationId(clientA1Id, orgA)).thenReturn(Optional.of(clientA1));
            when(clientRepository.findByIdAndOrganizationId(clientA2Id, orgA)).thenReturn(Optional.of(clientA2));
            when(clientRepository.findByIdAndOrganizationId(clientAUnassignedId, orgA)).thenReturn(Optional.of(clientAUnassigned));

            // 1. Access assigned client -> 200 OK
            assertNotNull(clientService.getClientById(clientA1Id));

            // 2. Access colleague's client -> 403 Forbidden
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientA2Id));

            // 3. Access unassigned client -> 403 Forbidden
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientAUnassignedId));
        }

        @Test
        @DisplayName("Staff A1 cannot mutate or add notes to unassigned or unlinked clients")
        void testStaffCannotMutateUnassignedClient() {
            authenticateAs(staffA1UserId, orgA, "staff1@orga.com", Set.of("STAFF", "ROLE_STAFF"));

            when(employeeRepository.findByOrganizationIdAndUserId(orgA, staffA1UserId)).thenReturn(Optional.of(empStaffA1));
            when(clientRepository.findIdsByOrganizationIdAndAssignedEmployeeIdIn(eq(orgA), any()))
                    .thenReturn(List.of(clientA3Id));

            when(clientRepository.findByIdAndOrganizationId(clientA2Id, orgA)).thenReturn(Optional.of(clientA2));

            // Mutation attempts on Client A2 must throw AccessDeniedException
            UpdateClientRequest updateReq = UpdateClientRequest.builder().displayName("Hacked Name").build();
            assertThrows(AccessDeniedException.class, () -> clientService.updateClient(clientA2Id, updateReq));

            CreateClientNoteRequest noteReq = CreateClientNoteRequest.builder().title("Note").content("Content").build();
            assertThrows(AccessDeniedException.class, () -> clientService.addClientNote(clientA2Id, noteReq));
        }
    }

    // =========================================================================
    // 5. CLIENT PORTAL USER SCOPE TESTS
    // =========================================================================
    @Nested
    @DisplayName("5. Client Portal User Scope")
    class ClientPortalUserScopeTests {

        @Test
        @DisplayName("Client Portal user accesses only their own client record; access to others is denied")
        void testClientPortalUserScope() {
            authenticateAs(clientPortalUserId, orgA, "portal@clienta1.com", Set.of("CLIENT_USER", "ROLE_CLIENT_USER"), clientA1Id);

            when(clientRepository.findByIdAndOrganizationId(clientA1Id, orgA)).thenReturn(Optional.of(clientA1));
            when(clientRepository.findByIdAndOrganizationId(clientA2Id, orgA)).thenReturn(Optional.of(clientA2));

            PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
            Set<UUID> accessibleIds = securityScopeEvaluator.getAccessibleClientIds(scope);

            assertTrue(accessibleIds.contains(clientA1Id));
            assertFalse(accessibleIds.contains(clientA2Id));

            // Access own client -> Allowed
            assertNotNull(clientService.getClientById(clientA1Id));

            // Access another client -> Denied
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientA2Id));
        }
    }

    // =========================================================================
    // 6. TASK ASSIGNMENT SEPARATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("6. Task Assignment Scope Separation")
    class TaskAssignmentSeparationTests {

        @Test
        @DisplayName("INVARIANT: Assigning a task to Staff on Client A2 does NOT grant client portfolio access to Client A2")
        void testTaskAssignmentDoesNotGrantClientPortfolioAccess() {
            // Staff A1 is assigned a task on Client A2 (Client A2 is assigned to Practitioner A2, not Staff A1)
            authenticateAs(staffA1UserId, orgA, "staff1@orga.com", Set.of("STAFF", "ROLE_STAFF"));

            when(employeeRepository.findByOrganizationIdAndUserId(orgA, staffA1UserId)).thenReturn(Optional.of(empStaffA1));

            // Staff A1 is only assigned Client A3 in client portfolio
            when(clientRepository.findIdsByOrganizationIdAndAssignedEmployeeIdIn(eq(orgA), any()))
                    .thenReturn(List.of(clientA3Id));

            when(clientRepository.findByIdAndOrganizationId(clientA2Id, orgA)).thenReturn(Optional.of(clientA2));

            PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
            Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);

            // Client portfolio contains Client A3 only
            assertTrue(accessibleClientIds.contains(clientA3Id));
            assertFalse(accessibleClientIds.contains(clientA2Id));

            // Direct client portfolio lookup on Client A2 throws AccessDeniedException
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientA2Id));
            assertThrows(AccessDeniedException.class, () -> clientService.getClientOverview(clientA2Id));
        }

        @Test
        @DisplayName("INVARIANT: Creating or assigning a task does NOT alter Client.assignedEmployeeId")
        void testTaskCreationDoesNotAlterClientAssignedEmployee() {
            authenticateAs(ownerAUserId, orgA, "owner@orga.com", Set.of("ORG_ADMIN", "ROLE_ORG_ADMIN"));

            when(clientRepository.findByIdAndOrganizationId(clientA2Id, orgA)).thenReturn(Optional.of(clientA2));
            when(employeeRepository.findByIdAndOrganizationId(staffA1EmpId, orgA)).thenReturn(Optional.of(empStaffA1));

            UUID originalAssignedEmployeeId = clientA2.getAssignedEmployeeId();
            assertEquals(practitionerA2EmpId, originalAssignedEmployeeId);

            CreateTaskRequest taskRequest = CreateTaskRequest.builder()
                    .clientId(clientA2Id)
                    .assignedTo(staffA1EmpId) // Task assigned to Staff A1
                    .title("Prepare GST Return for Client A2")
                    .taskCategory(TaskEntity.TaskCategory.GST)
                    .build();

            when(taskRepository.save(any(TaskEntity.class))).thenAnswer(inv -> {
                TaskEntity t = inv.getArgument(0);
                t.setId(UUID.randomUUID());
                return t;
            });
            when(taskMapper.toDto(any(TaskEntity.class))).thenReturn(new com.taxoryn.module.task.dto.TaskDto());

            taskService.createTask(taskRequest);

            // Verify client's assigned employee was NEVER changed
            assertEquals(practitionerA2EmpId, clientA2.getAssignedEmployeeId());
            verify(clientRepository, never()).save(any(ClientEntity.class));
        }
    }

    // =========================================================================
    // 7. ORGANIZATION TYPE INDEPENDENCE TESTS
    // =========================================================================
    @Nested
    @DisplayName("7. OrganizationType Independence & Non-Interference")
    class OrganizationTypeIndependenceTests {

        @Test
        @DisplayName("INVARIANT: Security scope rules are identical regardless of OrganizationType")
        void testOrganizationTypeDoesNotAffectSecurityScope() {
            for (OrganizationType type : OrganizationType.values()) {
                OrganizationEntity org = OrganizationEntity.builder()
                        .name("Org Test " + type.name())
                        .organizationType(type)
                        .build();
                org.setId(orgA);

                // For practitioner A1 under this organization type
                authenticateAs(practitionerA1UserId, orgA, "prac1@orga.com", Set.of("PRACTITIONER", "ROLE_PRACTITIONER"));
                when(employeeRepository.findByOrganizationIdAndUserId(orgA, practitionerA1UserId)).thenReturn(Optional.of(empPractitionerA1));
                when(clientRepository.findIdsByOrganizationIdAndAssignedEmployeeIdIn(eq(orgA), any()))
                        .thenReturn(List.of(clientA1Id));

                PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

                assertEquals(PracticeSecurityScope.RoleTier.STAFF_INDIVIDUAL, scope.getRoleTier(),
                        "Role tier must be STAFF_INDIVIDUAL for OrganizationType: " + type);
                assertFalse(scope.isFirmAdmin(),
                        "isFirmAdmin must be false for OrganizationType: " + type);
                assertFalse(scope.isDepartmentManager(),
                        "isDepartmentManager must be false for OrganizationType: " + type);
            }
        }
    }
}
