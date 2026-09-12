package com.taxoryn.module.client.service;

import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.dto.AssignClientEmployeeRequest;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.ClientNoteDto;
import com.taxoryn.module.client.dto.ClientOverviewDto;
import com.taxoryn.module.client.dto.CreateClientNoteRequest;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientStatusRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.entity.ClientNoteEntity;
import com.taxoryn.module.client.entity.ClientNoteEntity.NoteType;
import com.taxoryn.module.client.mapper.ClientMapper;
import com.taxoryn.module.client.repository.ClientNoteRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.task.mapper.TaskMapper;
import com.taxoryn.module.task.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.taxoryn.module.role.entity.RoleEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientNoteRepository clientNoteRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ClientMapper clientMapper;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private com.taxoryn.module.subscription.service.SubscriptionService subscriptionService;

    @Mock
    private com.taxoryn.module.audit.service.AuditService auditService;

    @Mock
    private com.taxoryn.core.security.PracticeSecurityScopeEvaluator securityScopeEvaluator;

    @Mock
    private com.taxoryn.module.user.repository.UserRepository userRepository;

    @Mock
    private com.taxoryn.module.authentication.repository.OrganizationActivationTokenRepository activationTokenRepository;

    @Mock
    private com.taxoryn.module.notification.email.service.EmailNotificationService emailNotificationService;

    @Mock
    private com.taxoryn.module.organization.repository.OrganizationRepository organizationRepository;

    @Mock
    private com.taxoryn.module.role.repository.RoleRepository roleRepository;

    @Mock
    private com.taxoryn.module.notice.repository.TaxNoticeRepository noticeRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    private UUID tenantId;
    private UUID clientId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .organizationId(tenantId)
                .email("admin@taxpractice.com")
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("CLIENT_CREATE", "CLIENT_VIEW", "CLIENT_UPDATE"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(tenantId);

        org.mockito.Mockito.lenient().when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(com.taxoryn.core.security.PracticeSecurityScope.firmAdmin(userId));
        org.mockito.Mockito.lenient().when(securityScopeEvaluator.hasBillingAccess(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Create client successfully")
    void testCreateClientSuccess() {
        CreateClientRequest request = CreateClientRequest.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Zenith Infotech Pvt Ltd")
                .pan("AAACZ1234D")
                .gstin("27AAACZ1234D1Z8")
                .tan("MUMZ12345A")
                .cin("U72200MH2018PTC312345")
                .city("Mumbai")
                .state("Maharashtra")
                .status(ClientStatus.ACTIVE)
                .build();

        when(clientRepository.existsByOrganizationIdAndPan(tenantId, "AAACZ1234D")).thenReturn(false);
        when(clientRepository.existsByOrganizationIdAndGstin(tenantId, "27AAACZ1234D1Z8")).thenReturn(false);

        ClientEntity saved = ClientEntity.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Zenith Infotech Pvt Ltd")
                .pan("AAACZ1234D")
                .gstin("27AAACZ1234D1Z8")
                .tan("MUMZ12345A")
                .cin("U72200MH2018PTC312345")
                .city("Mumbai")
                .state("Maharashtra")
                .status(ClientStatus.ACTIVE)
                .build();
        saved.setId(clientId);
        saved.setOrganizationId(tenantId);

        when(clientRepository.save(any(ClientEntity.class))).thenReturn(saved);
        when(clientMapper.toDto(saved)).thenReturn(ClientDto.builder()
                .id(clientId)
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Zenith Infotech Pvt Ltd")
                .pan("AAACZ1234D")
                .gstin("27AAACZ1234D1Z8")
                .build());

        ClientDto result = clientService.createClient(request);

        assertNotNull(result);
        assertEquals("Zenith Infotech Pvt Ltd", result.getDisplayName());
        assertEquals("AAACZ1234D", result.getPan());
    }

    @Test
    @DisplayName("Create client duplicate PAN throws DuplicateResourceException")
    void testCreateClientDuplicatePanThrows() {
        CreateClientRequest request = CreateClientRequest.builder()
                .clientType(ClientType.INDIVIDUAL)
                .displayName("Anand Joshi")
                .pan("ABCPJ9876M")
                .build();

        when(clientRepository.existsByOrganizationIdAndPan(tenantId, "ABCPJ9876M")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> clientService.createClient(request));
    }

    @Test
    @DisplayName("Assign employee to client")
    void testAssignEmployee() {
        ClientEntity client = ClientEntity.builder()
                .displayName("Zenith Infotech Pvt Ltd")
                .build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        EmployeeEntity employee = EmployeeEntity.builder()
                .firstName("Amit")
                .lastName("Sharma")
                .build();
        employee.setId(employeeId);
        employee.setOrganizationId(tenantId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(employeeRepository.findByIdAndOrganizationId(employeeId, tenantId)).thenReturn(Optional.of(employee));
        when(clientRepository.save(client)).thenReturn(client);
        when(clientMapper.toDto(client)).thenReturn(ClientDto.builder().id(clientId).assignedEmployeeId(employeeId).build());

        ClientDto result = clientService.assignEmployee(clientId, new AssignClientEmployeeRequest(employeeId));

        assertNotNull(result);
        assertEquals(employeeId, result.getAssignedEmployeeId());
    }

    @Test
    @DisplayName("Get Client 360-Degree Overview returns aggregated metrics")
    void testGetClientOverview() {
        ClientEntity client = ClientEntity.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Zenith Infotech Pvt Ltd")
                .pan("AAACZ1234D")
                .gstin("27AAACZ1234D1Z8")
                .status(ClientStatus.ACTIVE)
                .build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(clientMapper.toDto(client)).thenReturn(ClientDto.builder().id(clientId).displayName("Zenith Infotech Pvt Ltd").build());
        when(taskRepository.findAllByOrganizationIdAndClientId(eq(tenantId), eq(clientId), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(clientNoteRepository.findTop10ByOrganizationIdAndClientIdOrderByCreatedAtDesc(tenantId, clientId))
                .thenReturn(List.of());
        when(noticeRepository.findAllByOrganizationIdAndClientId(tenantId, clientId))
                .thenReturn(List.of());

        ClientOverviewDto overview = clientService.getClientOverview(clientId);

        assertNotNull(overview);
        assertNotNull(overview.getClient());
        assertNotNull(overview.getStatutory());
        assertNotNull(overview.getTaskSummary());
        assertNotNull(overview.getComplianceSummary());
        assertEquals("Zenith Infotech Pvt Ltd", overview.getClient().getDisplayName());
    }

    @Test
    @DisplayName("Add communication note for client")
    void testAddClientNote() {
        CreateClientNoteRequest request = CreateClientNoteRequest.builder()
                .noteType(NoteType.MEETING)
                .title("Annual Audit Scope")
                .content("Agreed on audit timeline.")
                .build();

        ClientEntity client = ClientEntity.builder().displayName("Zenith").build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));

        ClientNoteEntity saved = ClientNoteEntity.builder()
                .clientId(clientId)
                .noteType(NoteType.MEETING)
                .title("Annual Audit Scope")
                .content("Agreed on audit timeline.")
                .build();
        saved.setId(UUID.randomUUID());
        saved.setOrganizationId(tenantId);

        when(clientNoteRepository.save(any(ClientNoteEntity.class))).thenReturn(saved);
        when(clientMapper.toNoteDto(saved)).thenReturn(ClientNoteDto.builder()
                .id(saved.getId())
                .title("Annual Audit Scope")
                .noteType(NoteType.MEETING)
                .build());

        ClientNoteDto result = clientService.addClientNote(clientId, request);

        assertNotNull(result);
        assertEquals("Annual Audit Scope", result.getTitle());
    }

    @Test
    @DisplayName("Create client with email auto-provisions portal user and dispatches activation email")
    void testCreateClientWithEmail_AutoProvisionsPortalUserAndSendsActivationEmail() {
        CreateClientRequest request = CreateClientRequest.builder()
                .clientType(ClientType.INDIVIDUAL)
                .displayName("Rajesh Kumar")
                .email("rajesh.kumar@example.com")
                .phone("9876543210")
                .pan("ABCDE1234F")
                .status(ClientStatus.ACTIVE)
                .build();

        when(clientRepository.existsByOrganizationIdAndPan(tenantId, "ABCDE1234F")).thenReturn(false);

        ClientEntity saved = ClientEntity.builder()
                .clientType(ClientType.INDIVIDUAL)
                .displayName("Rajesh Kumar")
                .email("rajesh.kumar@example.com")
                .phone("9876543210")
                .pan("ABCDE1234F")
                .status(ClientStatus.ACTIVE)
                .build();
        saved.setId(clientId);
        saved.setOrganizationId(tenantId);

        when(clientRepository.save(any(ClientEntity.class))).thenReturn(saved);
        when(clientMapper.toDto(saved)).thenReturn(ClientDto.builder()
                .id(clientId)
                .displayName("Rajesh Kumar")
                .email("rajesh.kumar@example.com")
                .build());
        when(userRepository.findByOrganizationIdAndEmailIgnoreCase(tenantId, "rajesh.kumar@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_USER")).thenReturn(Optional.of(
                RoleEntity.builder().code("CLIENT_USER").name("Client User").isSystemRole(true).build()
        ));
        when(organizationRepository.findById(tenantId)).thenReturn(Optional.of(
                com.taxoryn.module.organization.entity.OrganizationEntity.builder()
                        .name("Apex Tax Consultants")
                        .build()
        ));

        com.taxoryn.module.user.entity.UserEntity provisionedUser = com.taxoryn.module.user.entity.UserEntity.builder()
                .email("rajesh.kumar@example.com")
                .firstName("Rajesh")
                .lastName("Kumar")
                .status(com.taxoryn.module.user.entity.UserEntity.UserStatus.INVITED)
                .organizationId(tenantId)
                .clientId(clientId)
                .build();
        provisionedUser.setId(UUID.randomUUID());
        when(userRepository.save(any(com.taxoryn.module.user.entity.UserEntity.class))).thenReturn(provisionedUser);

        ClientDto result = clientService.createClient(request);

        assertNotNull(result);
        verify(userRepository).save(any(com.taxoryn.module.user.entity.UserEntity.class));
        verify(activationTokenRepository).save(any(com.taxoryn.module.authentication.entity.OrganizationActivationTokenEntity.class));
        verify(emailNotificationService).sendClientPortalInvitationEmail(
                eq("rajesh.kumar@example.com"),
                eq("Rajesh Kumar"),
                eq("Rajesh Kumar"),
                eq("Apex Tax Consultants"),
                org.mockito.ArgumentMatchers.contains("/activate?token="),
                eq(24L)
        );
    }

    @Test
    @DisplayName("Resend portal invitation invalidates prior tokens and dispatches new invitation email")
    void testResendPortalInvitation_Success() {
        ClientEntity client = ClientEntity.builder()
                .displayName("Zenith Infotech Pvt Ltd")
                .email("portal@zenithinfo.com")
                .build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        com.taxoryn.module.user.entity.UserEntity portalUser = com.taxoryn.module.user.entity.UserEntity.builder()
                .email("portal@zenithinfo.com")
                .firstName("Zenith")
                .lastName("Infotech Pvt Ltd")
                .status(com.taxoryn.module.user.entity.UserEntity.UserStatus.INVITED)
                .organizationId(tenantId)
                .clientId(clientId)
                .build();
        portalUser.setId(UUID.randomUUID());

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(userRepository.findAllByOrganizationIdAndClientId(tenantId, clientId)).thenReturn(List.of(portalUser));
        when(organizationRepository.findById(tenantId)).thenReturn(Optional.of(
                com.taxoryn.module.organization.entity.OrganizationEntity.builder()
                        .name("Apex Tax Consultants")
                        .build()
        ));

        clientService.resendPortalInvitation(clientId);

        verify(activationTokenRepository).invalidateAllPendingTokensForUser(eq(portalUser.getId()), any(Instant.class));
        verify(activationTokenRepository).save(any(com.taxoryn.module.authentication.entity.OrganizationActivationTokenEntity.class));
        verify(emailNotificationService).sendClientPortalInvitationEmail(
                eq("portal@zenithinfo.com"),
                eq("Zenith Infotech Pvt Ltd"),
                eq("Zenith Infotech Pvt Ltd"),
                eq("Apex Tax Consultants"),
                org.mockito.ArgumentMatchers.contains("/activate?token="),
                eq(24L)
        );
    }

    @Test
    @DisplayName("Resend portal invitation for unprovisioned client with email provisions portal user and dispatches invitation")
    void testResendPortalInvitation_NotProvisionedClient_ProvisionsAndDispatches() {
        ClientEntity client = ClientEntity.builder()
                .displayName("Ishani InfoTech")
                .email("ishanipatha25@gmail.com")
                .build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(userRepository.findAllByOrganizationIdAndClientId(tenantId, clientId)).thenReturn(List.of());
        when(userRepository.findByOrganizationIdAndEmailIgnoreCase(tenantId, "ishanipatha25@gmail.com")).thenReturn(Optional.empty());

        RoleEntity clientUserRole = RoleEntity.builder()
                .code("CLIENT_USER")
                .name("Client Portal User")
                .build();
        when(roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_USER")).thenReturn(Optional.of(clientUserRole));

        com.taxoryn.module.user.entity.UserEntity newlyProvisionedUser = com.taxoryn.module.user.entity.UserEntity.builder()
                .email("ishanipatha25@gmail.com")
                .firstName("Ishani")
                .lastName("InfoTech")
                .status(com.taxoryn.module.user.entity.UserEntity.UserStatus.INVITED)
                .organizationId(tenantId)
                .clientId(clientId)
                .build();
        newlyProvisionedUser.setId(UUID.randomUUID());
        when(userRepository.save(any(com.taxoryn.module.user.entity.UserEntity.class))).thenReturn(newlyProvisionedUser);

        when(organizationRepository.findById(tenantId)).thenReturn(Optional.of(
                com.taxoryn.module.organization.entity.OrganizationEntity.builder()
                        .name("Apex Tax Consultants")
                        .build()
        ));

        clientService.resendPortalInvitation(clientId);

        verify(userRepository).save(any(com.taxoryn.module.user.entity.UserEntity.class));
        verify(activationTokenRepository).save(any(com.taxoryn.module.authentication.entity.OrganizationActivationTokenEntity.class));
        verify(emailNotificationService).sendClientPortalInvitationEmail(
                eq("ishanipatha25@gmail.com"),
                eq("Ishani InfoTech"),
                eq("Ishani InfoTech"),
                eq("Apex Tax Consultants"),
                org.mockito.ArgumentMatchers.contains("/activate?token="),
                eq(24L)
        );
    }

    @Test
    @DisplayName("Resend portal invitation for ACTIVE user throws BadRequestException")
    void testResendPortalInvitation_ActiveUser_ThrowsBadRequest() {
        ClientEntity client = ClientEntity.builder()
                .displayName("Active Corp")
                .email("active@activecorp.com")
                .build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        com.taxoryn.module.user.entity.UserEntity activeUser = com.taxoryn.module.user.entity.UserEntity.builder()
                .email("active@activecorp.com")
                .status(com.taxoryn.module.user.entity.UserEntity.UserStatus.ACTIVE)
                .organizationId(tenantId)
                .clientId(clientId)
                .build();
        activeUser.setId(UUID.randomUUID());

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(userRepository.findAllByOrganizationIdAndClientId(tenantId, clientId)).thenReturn(List.of(activeUser));

        com.taxoryn.core.exception.BadRequestException ex = assertThrows(
                com.taxoryn.core.exception.BadRequestException.class,
                () -> clientService.resendPortalInvitation(clientId)
        );
        assertEquals("Client portal access is already active for this client. If they cannot log in, please guide them to use password recovery.", ex.getMessage());
    }

    @Test
    @DisplayName("Resend portal invitation for SUSPENDED user throws BadRequestException")
    void testResendPortalInvitation_SuspendedUser_ThrowsBadRequest() {
        ClientEntity client = ClientEntity.builder()
                .displayName("Suspended Corp")
                .email("suspended@corp.com")
                .build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        com.taxoryn.module.user.entity.UserEntity suspendedUser = com.taxoryn.module.user.entity.UserEntity.builder()
                .email("suspended@corp.com")
                .status(com.taxoryn.module.user.entity.UserEntity.UserStatus.SUSPENDED)
                .organizationId(tenantId)
                .clientId(clientId)
                .build();
        suspendedUser.setId(UUID.randomUUID());

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(userRepository.findAllByOrganizationIdAndClientId(tenantId, clientId)).thenReturn(List.of(suspendedUser));

        com.taxoryn.core.exception.BadRequestException ex = assertThrows(
                com.taxoryn.core.exception.BadRequestException.class,
                () -> clientService.resendPortalInvitation(clientId)
        );
        assertEquals("Client portal access is currently suspended for this client. Please restore portal access before resending an invitation.", ex.getMessage());
    }

    @Test
    @DisplayName("Provisioning user with staff email throws DuplicateResourceException")
    void testResendPortalInvitation_StaffEmail_ThrowsConflict() {
        ClientEntity client = ClientEntity.builder()
                .displayName("Staff Client")
                .email("staff@apextax.com")
                .build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        RoleEntity orgAdminRole = RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .build();

        com.taxoryn.module.user.entity.UserEntity staffUser = com.taxoryn.module.user.entity.UserEntity.builder()
                .email("staff@apextax.com")
                .status(com.taxoryn.module.user.entity.UserEntity.UserStatus.ACTIVE)
                .organizationId(tenantId)
                .clientId(null)
                .roles(Set.of(orgAdminRole))
                .build();
        staffUser.setId(UUID.randomUUID());

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(userRepository.findAllByOrganizationIdAndClientId(tenantId, clientId)).thenReturn(List.of());
        when(userRepository.findByOrganizationIdAndEmailIgnoreCase(tenantId, "staff@apextax.com")).thenReturn(Optional.of(staffUser));

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> clientService.resendPortalInvitation(clientId)
        );
        assertEquals("This email address is already registered as an internal practice user. A separate client portal email address is required.", ex.getMessage());
    }

    @Test
    @DisplayName("Case B: Resending portal invitation reconciles orphan CLIENT_USER with null clientId")
    void testResendPortalInvitation_CaseB_OrphanClientUserReconciles() {
        ClientEntity client = ClientEntity.builder()
                .displayName("Ishani InfoTech")
                .email("ishanipatha25@gmail.com")
                .build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        RoleEntity clientUserRole = RoleEntity.builder()
                .code("CLIENT_USER")
                .name("Client User")
                .build();

        com.taxoryn.module.user.entity.UserEntity orphanUser = com.taxoryn.module.user.entity.UserEntity.builder()
                .email("ishanipatha25@gmail.com")
                .status(com.taxoryn.module.user.entity.UserEntity.UserStatus.INVITED)
                .organizationId(tenantId)
                .clientId(null) // Orphan
                .roles(Set.of(clientUserRole))
                .build();
        orphanUser.setId(UUID.randomUUID());

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(userRepository.findAllByOrganizationIdAndClientId(tenantId, clientId)).thenReturn(List.of());
        when(userRepository.findByOrganizationIdAndEmailIgnoreCase(tenantId, "ishanipatha25@gmail.com")).thenReturn(Optional.of(orphanUser));
        when(userRepository.save(any(com.taxoryn.module.user.entity.UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        clientService.resendPortalInvitation(clientId);

        assertEquals(clientId, orphanUser.getClientId());
        verify(emailNotificationService).sendClientPortalInvitationEmail(eq("ishanipatha25@gmail.com"), any(), eq("Ishani InfoTech"), any(), any(), eq(24L));
    }

    @Test
    @DisplayName("Case C: Provisioning user with email linked to another client throws DuplicateResourceException")
    void testResendPortalInvitation_CaseC_LinkedToOtherClientThrowsConflict() {
        UUID otherClientId = UUID.randomUUID();
        ClientEntity client = ClientEntity.builder()
                .displayName("Ishani InfoTech")
                .email("duplicate@client.com")
                .build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        RoleEntity clientUserRole = RoleEntity.builder()
                .code("CLIENT_USER")
                .name("Client User")
                .build();

        com.taxoryn.module.user.entity.UserEntity otherClientUser = com.taxoryn.module.user.entity.UserEntity.builder()
                .email("duplicate@client.com")
                .status(com.taxoryn.module.user.entity.UserEntity.UserStatus.ACTIVE)
                .organizationId(tenantId)
                .clientId(otherClientId)
                .roles(Set.of(clientUserRole))
                .build();
        otherClientUser.setId(UUID.randomUUID());

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(userRepository.findAllByOrganizationIdAndClientId(tenantId, clientId)).thenReturn(List.of());
        when(userRepository.findByOrganizationIdAndEmailIgnoreCase(tenantId, "duplicate@client.com")).thenReturn(Optional.of(otherClientUser));

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> clientService.resendPortalInvitation(clientId)
        );
        assertEquals("This email address is already associated with another client portal account in this practice. Please use the existing account or another email address.", ex.getMessage());
    }

    @Test
    @DisplayName("Case F: Provisioning user with email registered in different organization throws DuplicateResourceException")
    void testResendPortalInvitation_CaseF_ForeignOrgThrowsConflict() {
        UUID foreignOrgId = UUID.randomUUID();
        ClientEntity client = ClientEntity.builder()
                .displayName("Ishani InfoTech")
                .email("foreign@external.com")
                .build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        com.taxoryn.module.user.entity.UserEntity foreignUser = com.taxoryn.module.user.entity.UserEntity.builder()
                .email("foreign@external.com")
                .organizationId(foreignOrgId)
                .build();
        foreignUser.setId(UUID.randomUUID());

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(userRepository.findAllByOrganizationIdAndClientId(tenantId, clientId)).thenReturn(List.of());
        when(userRepository.findByOrganizationIdAndEmailIgnoreCase(tenantId, "foreign@external.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("foreign@external.com")).thenReturn(Optional.of(foreignUser));

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> clientService.resendPortalInvitation(clientId)
        );
        assertEquals("The email address is already registered and cannot be used for this client portal account.", ex.getMessage());
    }
}
