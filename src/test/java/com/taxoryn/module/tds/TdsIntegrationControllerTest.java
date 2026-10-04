package com.taxoryn.module.tds;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TdsIntegrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private GovernmentConnectionService govConnectionService;

    private OrganizationEntity organization;
    private UserEntity adminUser;
    private String adminToken;
    private GovConnectionDto tdsConnection;

    @BeforeEach
    void setUp() {
        organization = organizationRepository.save(OrganizationEntity.builder()
                .name("TDS Controller Test Org " + UUID.randomUUID())
                .email("tds.controller." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN_" + UUID.randomUUID().toString().substring(0, 8))
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        TenantContext.setTenantId(organization.getId());

        adminUser = userRepository.save(UserEntity.builder()
                .email("admin." + UUID.randomUUID() + "@taxoryn.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Tds")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                organization.getId(),
                adminUser.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("TDS_VIEW", "TDS_READ", "TDS_WRITE", "TDS_CREATE")
        );

        tdsConnection = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TDS)
                .displayName("TRACES Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(tdsConnection.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("traces_ctrl_***")
                .rawSecret("TracesSecretKey123")
                .build());

        govConnectionService.activateConnection(tdsConnection.getId());

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("POST /api/v1/tds/connections/{connectionId}/handshake must succeed with 200 OK")
    void testHandshakeEndpointSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/tds/connections/" + tdsConnection.getId() + "/handshake")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.connectionId").value(tdsConnection.getId().toString()))
                .andExpect(jsonPath("$.data.healthStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.data.message").value("TDS TRACES Gateway handshake successful"));
    }

    @Test
    @DisplayName("POST /api/v1/tds/connections/{connectionId}/handshake with mockOutcome=AUTH_REQUIRED")
    void testHandshakeEndpointAuthRequired() throws Exception {
        Map<String, Object> directives = Map.of("mockOutcome", "AUTH_REQUIRED");

        mockMvc.perform(post("/api/v1/tds/connections/" + tdsConnection.getId() + "/handshake")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(directives)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.healthStatus").value("AUTH_REQUIRED"))
                .andExpect(jsonPath("$.data.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/tds/connections/{connectionId}/handshake unauthenticated must return 401 Unauthorized")
    void testHandshakeEndpointUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/tds/connections/" + tdsConnection.getId() + "/handshake")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
