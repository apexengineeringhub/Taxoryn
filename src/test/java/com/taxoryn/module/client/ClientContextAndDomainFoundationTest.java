package com.taxoryn.module.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.dto.Client360Dto;
import com.taxoryn.module.client.dto.ClientContextSummaryDto;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientStatusRequest;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.service.ClientContextService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ClientContextAndDomainFoundationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientService clientService;

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
                .name("CA Firm Alpha " + UUID.randomUUID())
                .email("alpha." + UUID.randomUUID() + "@taxoryn.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        tenantB = organizationRepository.save(OrganizationEntity.builder()
                .name("CA Firm Beta " + UUID.randomUUID())
                .email("beta." + UUID.randomUUID() + "@taxoryn.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        adminA = userRepository.save(UserEntity.builder()
                .organizationId(tenantA.getId())
                .email("admin." + UUID.randomUUID() + "@alpha.test")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Alpha")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        adminB = userRepository.save(UserEntity.builder()
                .organizationId(tenantB.getId())
                .email("admin." + UUID.randomUUID() + "@beta.test")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Beta")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        tokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminA.getId(), tenantA.getId(), adminA.getEmail(),
                Set.of("ROLE_ORG_ADMIN", "ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE")
        );

        tokenB = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminB.getId(), tenantB.getId(), adminB.getEmail(),
                Set.of("ROLE_ORG_ADMIN", "ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE")
        );

        com.taxoryn.core.security.SecurityUser principalA = com.taxoryn.core.security.SecurityUser.builder()
                .userId(adminA.getId())
                .organizationId(tenantA.getId())
                .email(adminA.getEmail())
                .roles(Set.of("ROLE_ORG_ADMIN", "ORG_ADMIN"))
                .permissions(Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE"))
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
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Phase 28.1 - Client creation, retrieval, and update within organization scope")
    void testClientLifecycle() {
        CreateClientRequest req = CreateClientRequest.builder()
                .displayName("Acme Technologies Pvt Ltd")
                .legalName("Acme Technologies Private Limited")
                .clientType(ClientType.PRIVATE_LIMITED)
                .clientCode("CL-ACM-01")
                .pan("ABCDE1234F")
                .gstin("27ABCDE1234F1Z5")
                .email("contact@acme.test")
                .phone("9876543210")
                .city("Mumbai")
                .state("Maharashtra")
                .country("India")
                .pincode("400001")
                .build();

        ClientDto created = clientService.createClient(req);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getDisplayName()).isEqualTo("Acme Technologies Pvt Ltd");
        assertThat(created.getPan()).isEqualTo("ABCDE1234F");
        assertThat(created.getStatus()).isEqualTo(ClientStatus.ACTIVE);

        // Retrieve
        ClientDto retrieved = clientService.getClientById(created.getId());
        assertThat(retrieved.getId()).isEqualTo(created.getId());
        assertThat(retrieved.getLegalName()).isEqualTo("Acme Technologies Private Limited");

        // Update
        UpdateClientRequest updateReq = UpdateClientRequest.builder()
                .displayName("Acme Tech Solutions Pvt Ltd")
                .legalName("Acme Technologies Private Limited")
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan("ABCDE1234F")
                .email("info@acme.test")
                .build();

        ClientDto updated = clientService.updateClient(created.getId(), updateReq);
        assertThat(updated.getDisplayName()).isEqualTo("Acme Tech Solutions Pvt Ltd");
        assertThat(updated.getEmail()).isEqualTo("info@acme.test");
    }

    @Test
    @DisplayName("Phase 28.1 - ClientContextService contract resolution without entity coupling")
    void testClientContextServiceContract() {
        CreateClientRequest req = CreateClientRequest.builder()
                .displayName("Nexus Logistics LLP")
                .legalName("Nexus Logistics Limited Liability Partnership")
                .clientType(ClientType.LLP)
                .clientCode("CL-NEX-01")
                .pan("AAALN1234K")
                .gstin("27AAALN1234K1Z9")
                .email("ops@nexus.test")
                .phone("9123456780")
                .build();

        ClientDto created = clientService.createClient(req);

        // Explicit tenant query
        Optional<ClientContextSummaryDto> optContext = clientContextService.findClientContext(tenantA.getId(), created.getId());
        assertThat(optContext).isPresent();
        ClientContextSummaryDto summary = optContext.get();
        assertThat(summary.getClientId()).isEqualTo(created.getId());
        assertThat(summary.getOrganizationId()).isEqualTo(tenantA.getId());
        assertThat(summary.getDisplayName()).isEqualTo("Nexus Logistics LLP");
        assertThat(summary.getClientType()).isEqualTo(ClientType.LLP);
        assertThat(summary.isActive()).isTrue();
        assertThat(summary.getPan()).isEqualTo("AAALN1234K");

        // Implicit current tenant query
        ClientContextSummaryDto requireSummary = clientContextService.requireClientContext(created.getId());
        assertThat(requireSummary.getClientId()).isEqualTo(created.getId());

        // Existence and active checks
        assertThat(clientContextService.exists(tenantA.getId(), created.getId())).isTrue();
        assertThat(clientContextService.isActive(tenantA.getId(), created.getId())).isTrue();
        assertThat(clientContextService.exists(tenantA.getId(), UUID.randomUUID())).isFalse();

        // Cross-tenant context isolation
        assertThat(clientContextService.exists(tenantB.getId(), created.getId())).isFalse();
        assertThat(clientContextService.findClientContext(tenantB.getId(), created.getId())).isEmpty();
        assertThatThrownBy(() -> clientContextService.requireClientContext(tenantB.getId(), created.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Phase 28.1 - Client 360 Foundation Read Model retrieval")
    void testClient360ReadModel() {
        CreateClientRequest req = CreateClientRequest.builder()
                .displayName("Starlight Ventures")
                .clientType(ClientType.INDIVIDUAL)
                .pan("BNZPV9876Q")
                .email("star@ventures.test")
                .build();

        ClientDto created = clientService.createClient(req);

        Client360Dto client360 = clientService.getClient360(created.getId());
        assertThat(client360).isNotNull();
        assertThat(client360.getClient()).isNotNull();
        assertThat(client360.getClient().getId()).isEqualTo(created.getId());
        assertThat(client360.getIdentifiers()).isNotNull();
        assertThat(client360.getIdentifiers().getPan()).isEqualTo("BNZPV9876Q");
        assertThat(client360.getStatus()).isEqualTo(ClientStatus.ACTIVE);
    }

    @Test
    @DisplayName("Phase 28.1 - HTTP REST endpoints: GET /api/v1/clients/{id}/context and /360 with tenant isolation")
    void testRestEndpointsAndTenantIsolation() throws Exception {
        CreateClientRequest req = CreateClientRequest.builder()
                .displayName("Alpha Private Client")
                .clientType(ClientType.INDIVIDUAL)
                .pan("ABCPA1111A")
                .build();

        ClientDto created = clientService.createClient(req);

        // 1. Tenant A accesses its client context via REST
        mockMvc.perform(get("/api/v1/clients/" + created.getId() + "/context")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientId").value(created.getId().toString()))
                .andExpect(jsonPath("$.data.displayName").value("Alpha Private Client"));

        // 2. Tenant A accesses Client 360
        mockMvc.perform(get("/api/v1/clients/" + created.getId() + "/360")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.client.id").value(created.getId().toString()));

        // 3. Tenant B attempts to access Tenant A's client context -> 404 NOT FOUND
        mockMvc.perform(get("/api/v1/clients/" + created.getId() + "/context")
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());

        // 4. Tenant B attempts to access Tenant A's Client 360 -> 404 NOT FOUND
        mockMvc.perform(get("/api/v1/clients/" + created.getId() + "/360")
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());

        // 5. Tenant B attempts to access Tenant A's client profile -> 404 NOT FOUND
        mockMvc.perform(get("/api/v1/clients/" + created.getId())
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Phase 28.1 - Client status deactivation reflects in context active flag")
    void testClientDeactivationInContext() {
        CreateClientRequest req = CreateClientRequest.builder()
                .displayName("Temporary Consultancy")
                .clientType(ClientType.PROPRIETORSHIP)
                .build();

        ClientDto created = clientService.createClient(req);
        assertThat(clientContextService.isActive(tenantA.getId(), created.getId())).isTrue();

        clientService.updateClientStatus(created.getId(), UpdateClientStatusRequest.builder()
                .status(ClientStatus.INACTIVE)
                .build());

        ClientContextSummaryDto summary = clientContextService.requireClientContext(created.getId());
        assertThat(summary.getStatus()).isEqualTo(ClientStatus.INACTIVE);
        assertThat(summary.isActive()).isFalse();
        assertThat(clientContextService.isActive(tenantA.getId(), created.getId())).isFalse();
    }
}
