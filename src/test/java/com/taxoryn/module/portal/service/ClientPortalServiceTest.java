package com.taxoryn.module.portal.service;

import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.dto.DocumentDownloadDto;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.document.service.DocumentService;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.portal.dto.ClientPortalDashboardDto;
import com.taxoryn.module.portal.dto.ClientPortalProfileDto;
import com.taxoryn.module.portal.dto.ClientPortalUserDto;
import com.taxoryn.module.portal.dto.RegisterClientPortalUserRequest;
import com.taxoryn.module.portal.dto.UpdateClientPortalProfileRequest;
import com.taxoryn.module.portal.mapper.ClientPortalMapper;
import com.taxoryn.module.portal.repository.ClientDocumentRequestRepository;
import com.taxoryn.module.portal.repository.ClientNotificationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.repository.TaskRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientPortalServiceTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private GstReturnFilingRepository gstReturnFilingRepository;
    @Mock
    private ItrReturnRepository itrReturnRepository;
    @Mock
    private DocumentService documentService;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ClientNotificationRepository notificationRepository;
    @Mock
    private ClientDocumentRequestRepository docRequestRepository;
    @Mock
    private com.taxoryn.module.billing.repository.InvoiceRepository invoiceRepository;
    @Mock
    private com.taxoryn.module.billing.mapper.InvoiceMapper invoiceMapper;
    @Mock
    private ClientPortalMapper mapper;

    @InjectMocks
    private ClientPortalServiceImpl portalService;

    private UUID tenantId;
    private UUID clientId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        userId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .organizationId(tenantId)
                .clientId(clientId)
                .email("client@abctraders.com")
                .roles(Set.of("CLIENT_ADMIN"))
                .permissions(Set.of("CLIENT_PORTAL_ACCESS", "CLIENT_PORTAL_DOCUMENT_UPLOAD", "CLIENT_PORTAL_DOCUMENT_VIEW", "CLIENT_PORTAL_PROFILE_VIEW", "CLIENT_PORTAL_STATUS_VIEW"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Register client portal user successfully")
    void testRegisterClientPortalUser() {
        RegisterClientPortalUserRequest request = RegisterClientPortalUserRequest.builder()
                .clientId(clientId)
                .email("finance@abctraders.com")
                .password("SecretPass123!")
                .firstName("Rohan")
                .lastName("Verma")
                .role("CLIENT_USER")
                .build();

        ClientEntity client = ClientEntity.builder()
                .displayName("ABC Traders")
                .build();
        client.setId(clientId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(userRepository.findByEmailIgnoreCase("finance@abctraders.com")).thenReturn(Optional.empty());
        when(roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_USER")).thenReturn(Optional.of(RoleEntity.builder().code("CLIENT_USER").build()));
        when(passwordEncoder.encode("SecretPass123!")).thenReturn("encodedHash");

        UserEntity savedUser = UserEntity.builder()
                .email("finance@abctraders.com")
                .firstName("Rohan")
                .lastName("Verma")
                .clientId(clientId)
                .build();
        savedUser.setId(UUID.randomUUID());
        savedUser.setOrganizationId(tenantId);

        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        ClientPortalUserDto result = portalService.registerClientPortalUser(request);

        assertNotNull(result);
        assertEquals("finance@abctraders.com", result.getEmail());
        assertEquals("ABC Traders", result.getClientName());
    }

    @Test
    @DisplayName("Get client dashboard successfully")
    void testGetDashboard() {
        ClientEntity client = ClientEntity.builder()
                .displayName("ABC Traders")
                .legalName("ABC Traders Private Limited")
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan("AAACB1234D")
                .gstin("27AAACB1234D1Z5")
                .build();
        client.setId(clientId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(gstReturnFilingRepository.findAllByOrganizationIdAndClientIdOrderByDueDateDesc(tenantId, clientId)).thenReturn(List.of());
        when(itrReturnRepository.findAllByOrganizationIdAndClientIdOrderByAssessmentYearDesc(tenantId, clientId)).thenReturn(List.of());
        when(taskRepository.findAllByOrganizationIdAndClientId(tenantId, clientId)).thenReturn(List.of());

        ClientPortalDashboardDto dashboard = portalService.getDashboard();

        assertNotNull(dashboard);
        assertEquals("ABC Traders", dashboard.getDisplayName());
        assertEquals("AAACB1234D", dashboard.getPan());
        assertEquals("27AAACB1234D1Z5", dashboard.getGstin());
    }

    @Test
    @DisplayName("Update profile details successfully")
    void testUpdateProfile() {
        ClientEntity client = ClientEntity.builder()
                .displayName("ABC Traders")
                .email("old@abctraders.com")
                .build();
        client.setId(clientId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(ClientEntity.class))).thenReturn(client);
        when(mapper.toProfileDto(client)).thenReturn(ClientPortalProfileDto.builder()
                .clientId(clientId)
                .displayName("ABC Traders")
                .email("updated@abctraders.com")
                .build());

        UpdateClientPortalProfileRequest updateReq = UpdateClientPortalProfileRequest.builder()
                .email("updated@abctraders.com")
                .phone("+91 9876543210")
                .city("Mumbai")
                .build();

        ClientPortalProfileDto updated = portalService.updateProfile(updateReq);

        assertNotNull(updated);
        assertEquals("updated@abctraders.com", updated.getEmail());
    }

    @Test
    @DisplayName("Client cannot download document belonging to another client (throws ForbiddenException)")
    void testDownloadForeignClientDocumentThrowsForbidden() {
        UUID otherClientId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        DocumentEntity foreignDoc = DocumentEntity.builder()
                .clientId(otherClientId)
                .documentType(DocumentType.PAN_CARD)
                .build();
        foreignDoc.setId(docId);
        foreignDoc.setOrganizationId(tenantId);

        when(documentRepository.findByIdAndOrganizationId(docId, tenantId)).thenReturn(Optional.of(foreignDoc));

        assertThrows(ForbiddenException.class, () -> portalService.downloadClientDocument(docId));
    }
}
