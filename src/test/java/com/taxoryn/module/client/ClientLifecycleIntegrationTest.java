package com.taxoryn.module.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.dto.Client360Dto;
import com.taxoryn.module.client.dto.ClientContextSummaryDto;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.ClientFilterRequest;
import com.taxoryn.module.client.dto.ClientLifecycleSummaryDto;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientStatusRequest;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.service.ClientContextService;
import com.taxoryn.module.client.service.ClientLifecycleService;
import com.taxoryn.module.client.service.ClientService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ClientLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ClientLifecycleService clientLifecycleService;

    @Autowired
    private ClientContextService clientContextService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity tenantA;
    private OrganizationEntity tenantB;
    private UserEntity adminA;
    private UserEntity adminB;
    private String tokenA;
    private String tokenB;
    private RoleEntity adminRole;

    @BeforeEach
    void setUp() {
        adminRole = roleRepository.save(RoleEntity.builder()
                .code("ROLE_ORG_ADMIN")
                .name("Practice Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        tenantA = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Advisory Services " + UUID.randomUUID())
                .email("apex." + UUID.randomUUID() + "@advisory.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        tenantB = organizationRepository.save(OrganizationEntity.builder()
                .name("Horizon Tax Partners " + UUID.randomUUID())
                .email("horizon." + UUID.randomUUID() + "@tax.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        adminA = userRepository.save(UserEntity.builder()
                .organizationId(tenantA.getId())
                .email("admin." + UUID.randomUUID() + "@apex.test")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Apex")
                .lastName("Principal")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        adminB = userRepository.save(UserEntity.builder()
                .organizationId(tenantB.getId())
                .email("admin." + UUID.randomUUID() + "@horizon.test")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Horizon")
                .lastName("Principal")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        tokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminA.getId(), tenantA.getId(), adminA.getEmail(),
                Set.of("ROLE_ORG_ADMIN", "ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE", "CLIENT_WRITE")
        );

        tokenB = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminB.getId(), tenantB.getId(), adminB.getEmail(),
                Set.of("ROLE_ORG_ADMIN", "ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE", "CLIENT_WRITE")
        );

        com.taxoryn.core.security.SecurityUser principalA = com.taxoryn.core.security.SecurityUser.builder()
                .userId(adminA.getId())
                .organizationId(tenantA.getId())
                .email(adminA.getEmail())
                .roles(Set.of("ROLE_ORG_ADMIN", "ORG_ADMIN"))
                .permissions(Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE", "CLIENT_WRITE"))
                .enabled(true)
                .build();

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principalA, null, principalA.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        TenantContext.setTenantId(tenantA.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Phase 28.3 - Authoritative Client Lifecycle State Retrieval")
    void testGetLifecycleSummary() {
        CreateClientRequest req = CreateClientRequest.builder()
                .displayName("Acme Global Services")
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan("AAACA1234A")
                .build();

        ClientDto client = clientService.createClient(req);

        ClientLifecycleSummaryDto lifecycle = clientLifecycleService.getLifecycleSummary(client.getId());
        assertThat(lifecycle).isNotNull();
        assertThat(lifecycle.getClientId()).isEqualTo(client.getId());
        assertThat(lifecycle.getOrganizationId()).isEqualTo(tenantA.getId());
        assertThat(lifecycle.getCurrentStatus()).isEqualTo(ClientStatus.ACTIVE);
        assertThat(lifecycle.isActive()).isTrue();
        assertThat(lifecycle.isArchived()).isFalse();
        assertThat(lifecycle.getAllowedTransitions()).contains(ClientStatus.INACTIVE, ClientStatus.SUSPENDED, ClientStatus.ARCHIVED);
    }

    @Test
    @DisplayName("Phase 28.3 - Controlled State Transitions with Status Metadata and Audit Reason")
    void testControlledLifecycleTransitions() {
        CreateClientRequest req = CreateClientRequest.builder()
                .displayName("Pinnacle Advisory LLP")
                .clientType(ClientType.LLP)
                .build();

        ClientDto client = clientService.createClient(req);

        // 1. ACTIVE -> INACTIVE with reason
        UpdateClientStatusRequest inactiveReq = UpdateClientStatusRequest.builder()
                .status(ClientStatus.INACTIVE)
                .reason("Client temporarily paused retainer engagement")
                .build();

        ClientDto inactiveClient = clientLifecycleService.transitionStatus(client.getId(), inactiveReq);
        assertThat(inactiveClient.getStatus()).isEqualTo(ClientStatus.INACTIVE);
        assertThat(inactiveClient.getStatusChangedAt()).isNotNull();
        assertThat(inactiveClient.getStatusChangedBy()).isEqualTo(adminA.getId());
        assertThat(inactiveClient.getStatusChangeReason()).isEqualTo("Client temporarily paused retainer engagement");

        // Verify context reflects inactivity
        ClientContextSummaryDto contextInactive = clientContextService.requireClientContext(client.getId());
        assertThat(contextInactive.getStatus()).isEqualTo(ClientStatus.INACTIVE);
        assertThat(contextInactive.isActive()).isFalse();
        assertThat(contextInactive.getStatusChangeReason()).isEqualTo("Client temporarily paused retainer engagement");

        // 2. INACTIVE -> ACTIVE
        UpdateClientStatusRequest reactivateReq = UpdateClientStatusRequest.builder()
                .status(ClientStatus.ACTIVE)
                .reason("Client renewed annual retainer")
                .build();

        ClientDto activeClient = clientLifecycleService.transitionStatus(client.getId(), reactivateReq);
        assertThat(activeClient.getStatus()).isEqualTo(ClientStatus.ACTIVE);
        assertThat(activeClient.getStatusChangeReason()).isEqualTo("Client renewed annual retainer");

        // Verify context reflects active
        ClientContextSummaryDto contextActive = clientContextService.requireClientContext(client.getId());
        assertThat(contextActive.getStatus()).isEqualTo(ClientStatus.ACTIVE);
        assertThat(contextActive.isActive()).isTrue();

        // 3. ACTIVE -> ARCHIVED
        UpdateClientStatusRequest archiveReq = UpdateClientStatusRequest.builder()
                .status(ClientStatus.ARCHIVED)
                .reason("Business dissolved - retain records for 8 years statutory limit")
                .build();

        ClientDto archivedClient = clientLifecycleService.transitionStatus(client.getId(), archiveReq);
        assertThat(archivedClient.getStatus()).isEqualTo(ClientStatus.ARCHIVED);
        assertThat(archivedClient.getStatusChangeReason()).isEqualTo("Business dissolved - retain records for 8 years statutory limit");

        ClientLifecycleSummaryDto summary = clientLifecycleService.getLifecycleSummary(client.getId());
        assertThat(summary.isArchived()).isTrue();
        assertThat(summary.isActive()).isFalse();
    }

    @Test
    @DisplayName("Phase 28.3 - Reject Invalid Transitions and Null Status deterministically")
    void testRejectInvalidTransitions() {
        CreateClientRequest req = CreateClientRequest.builder()
                .displayName("Strict Transition Corp")
                .clientType(ClientType.COMPANY)
                .build();

        ClientDto client = clientService.createClient(req);

        // Null status should be rejected
        assertThatThrownBy(() -> clientLifecycleService.transitionStatus(client.getId(), UpdateClientStatusRequest.builder().status(null).build()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Phase 28.3 - Client list status filtering")
    void testClientListFilteringByStatus() {
        // Create 1 Active, 1 Inactive, 1 Archived
        ClientDto c1 = clientService.createClient(CreateClientRequest.builder().displayName("Client Active 1").build());
        ClientDto c2 = clientService.createClient(CreateClientRequest.builder().displayName("Client Inactive 1").build());
        clientLifecycleService.transitionStatus(c2.getId(), UpdateClientStatusRequest.builder().status(ClientStatus.INACTIVE).build());

        ClientDto c3 = clientService.createClient(CreateClientRequest.builder().displayName("Client Archived 1").build());
        clientLifecycleService.transitionStatus(c3.getId(), UpdateClientStatusRequest.builder().status(ClientStatus.ARCHIVED).build());

        // Filter ACTIVE
        PagedResponse<ClientDto> activeList = clientService.getClients(ClientFilterRequest.builder().status(ClientStatus.ACTIVE).build());
        assertThat(activeList.getContent()).anyMatch(c -> c.getId().equals(c1.getId()));
        assertThat(activeList.getContent()).noneMatch(c -> c.getId().equals(c2.getId()) || c.getId().equals(c3.getId()));

        // Filter INACTIVE
        PagedResponse<ClientDto> inactiveList = clientService.getClients(ClientFilterRequest.builder().status(ClientStatus.INACTIVE).build());
        assertThat(inactiveList.getContent()).anyMatch(c -> c.getId().equals(c2.getId()));
        assertThat(inactiveList.getContent()).noneMatch(c -> c.getId().equals(c1.getId()));

        // Filter ARCHIVED
        PagedResponse<ClientDto> archivedList = clientService.getClients(ClientFilterRequest.builder().status(ClientStatus.ARCHIVED).build());
        assertThat(archivedList.getContent()).anyMatch(c -> c.getId().equals(c3.getId()));
    }

    @Test
    @DisplayName("Phase 28.3 - REST API: GET /lifecycle, PATCH /status, PUT /status with Tenant Isolation")
    void testRestLifecycleEndpointsAndTenantIsolation() throws Exception {
        CreateClientRequest req = CreateClientRequest.builder()
                .displayName("REST Lifecycle Enterprise")
                .clientType(ClientType.PRIVATE_LIMITED)
                .build();

        ClientDto created = clientService.createClient(req);

        // 1. GET /lifecycle (Tenant A) -> 200 OK
        mockMvc.perform(get("/api/v1/clients/" + created.getId() + "/lifecycle")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientId").value(created.getId().toString()))
                .andExpect(jsonPath("$.data.currentStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.active").value(true));

        // 2. PATCH /status (Tenant A) -> 200 OK
        UpdateClientStatusRequest patchReq = UpdateClientStatusRequest.builder()
                .status(ClientStatus.SUSPENDED)
                .reason("KYC re-verification pending")
                .build();

        mockMvc.perform(patch("/api/v1/clients/" + created.getId() + "/status")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.data.statusChangeReason").value("KYC re-verification pending"));

        // 3. PUT /status (Tenant A) -> 200 OK
        UpdateClientStatusRequest putReq = UpdateClientStatusRequest.builder()
                .status(ClientStatus.ACTIVE)
                .reason("KYC re-verification approved")
                .build();

        mockMvc.perform(put("/api/v1/clients/" + created.getId() + "/status")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(putReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.statusChangeReason").value("KYC re-verification approved"));

        // 4. Tenant B attempts GET /lifecycle -> 404 NOT FOUND
        mockMvc.perform(get("/api/v1/clients/" + created.getId() + "/lifecycle")
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());

        // 5. Tenant B attempts PATCH /status -> 404 NOT FOUND
        mockMvc.perform(patch("/api/v1/clients/" + created.getId() + "/status")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Phase 28.3 - Client 360 unified read model reflects lifecycle status metadata")
    void testClient360ReflectsLifecycleMetadata() {
        CreateClientRequest req = CreateClientRequest.builder()
                .displayName("Lifecycle 360 Client")
                .clientType(ClientType.INDIVIDUAL)
                .build();

        ClientDto created = clientService.createClient(req);

        clientLifecycleService.transitionStatus(created.getId(), UpdateClientStatusRequest.builder()
                .status(ClientStatus.INACTIVE)
                .reason("Client seasonal pause")
                .build());

        Client360Dto client360 = clientService.getClient360(created.getId());
        assertThat(client360).isNotNull();
        assertThat(client360.getStatus()).isEqualTo(ClientStatus.INACTIVE);
        assertThat(client360.getStatusChangedAt()).isNotNull();
        assertThat(client360.getStatusChangeReason()).isEqualTo("Client seasonal pause");
        assertThat(client360.getClient().getStatus()).isEqualTo(ClientStatus.INACTIVE);
    }
}
