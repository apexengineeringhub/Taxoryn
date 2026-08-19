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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

        SecurityUser principal = SecurityUser.builder()
                .userId(UUID.randomUUID())
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
}
