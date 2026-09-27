package com.taxoryn.module.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.dto.CreateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.UpdateObligationStatusRequest;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplianceObligationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComplianceObligationRepository obligationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private LocationRepository locationRepository;

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

    private OrganizationEntity org;
    private LocationEntity location;
    private UserEntity adminUser;
    private String adminToken;
    private ClientEntity client;

    @BeforeEach
    void setUp() {
        cleanUp();

        org = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Tax Advisors LLP")
                .legalName("Alpha Tax Advisors LLP")
                .status(OrganizationStatus.ACTIVE)
                .email("contact-" + UUID.randomUUID() + "@alphatax.in")
                .build());

        LocationEntity loc = LocationEntity.builder()
                .name("Mumbai Central Office")
                .code("MUM-01")
                .city("Mumbai")
                .state("Maharashtra")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        loc.setOrganizationId(org.getId());
        location = locationRepository.save(loc);

        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        adminUser = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("admin-" + UUID.randomUUID() + "@alphatax.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("Managing")
                .lastName("Partner")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                org.getId(),
                adminUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_EDIT", "TASK_VIEW", "TASK_CREATE", "TASK_EDIT", "GST_VIEW", "GST_EDIT")
        );

        ClientEntity clientEntity = ClientEntity.builder()
                .displayName("Acme Global Tech Solutions")
                .legalName("Acme Global Tech Solutions Pvt Ltd")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .pan("ABCDE1234F")
                .gstin("27ABCDE1234F1Z5")
                .build();
        clientEntity.setOrganizationId(org.getId());
        client = clientRepository.save(clientEntity);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        obligationRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Create obligation with location and assigned user scope and retrieve successfully")
    void testCreateAndRetrieveObligation() throws Exception {
        CreateComplianceObligationRequest request = CreateComplianceObligationRequest.builder()
                .clientId(client.getId())
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("GSTR-3B Filing for August 2026")
                .description("Monthly GST summary return")
                .periodLabel("August 2026")
                .financialYear("2026-27")
                .statutoryDueDate(LocalDate.of(2026, 9, 20))
                .internalTargetDate(LocalDate.of(2026, 9, 17))
                .priority(TaskPriority.HIGH)
                .assignedUserId(adminUser.getId())
                .locationId(location.getId())
                .notes("Verify 2B reconciliation before filing")
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/compliance/obligations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("GSTR-3B Filing for August 2026"))
                .andExpect(jsonPath("$.data.obligationType").value("GST_RETURN"))
                .andExpect(jsonPath("$.data.status").value("UPCOMING"))
                .andExpect(jsonPath("$.data.assignedUserId").value(adminUser.getId().toString()))
                .andExpect(jsonPath("$.data.locationId").value(location.getId().toString()))
                .andExpect(jsonPath("$.data.locationName").value("Mumbai Central Office"))
                .andReturn().getResponse().getContentAsString();

        UUID createdId = UUID.fromString(objectMapper.readTree(responseJson).get("data").get("id").asText());

        mockMvc.perform(get("/api/v1/compliance/obligations/" + createdId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdId.toString()))
                .andExpect(jsonPath("$.data.clientDisplayName").value("Acme Global Tech Solutions"))
                .andExpect(jsonPath("$.data.statutoryDueDate").value("2026-09-20"));
    }

    @Test
    @DisplayName("Update obligation status through lifecycle with transition validations")
    void testObligationStatusTransitions() throws Exception {
        ComplianceObligationEntity obligation = ComplianceObligationEntity.builder()
                .clientId(client.getId())
                .obligationType(ComplianceObligationType.ITR_FILING)
                .title("Corporate ITR-6 FY 2025-26")
                .periodLabel("AY 2026-27")
                .statutoryDueDate(LocalDate.of(2026, 10, 31))
                .status(ComplianceObligationStatus.UPCOMING)
                .priority(TaskPriority.HIGH)
                .build();
        obligation.setOrganizationId(org.getId());
        obligation = obligationRepository.save(obligation);

        // 1. UPCOMING -> IN_PROGRESS
        mockMvc.perform(put("/api/v1/compliance/obligations/" + obligation.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateObligationStatusRequest.builder()
                                .status(ComplianceObligationStatus.IN_PROGRESS)
                                .notes("Tax audit computation started")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        // 2. IN_PROGRESS -> READY_FOR_FILING
        mockMvc.perform(put("/api/v1/compliance/obligations/" + obligation.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateObligationStatusRequest.builder()
                                .status(ComplianceObligationStatus.READY_FOR_FILING)
                                .notes("Audit sign-off received")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY_FOR_FILING"));

        // 3. READY_FOR_FILING -> FILED
        mockMvc.perform(put("/api/v1/compliance/obligations/" + obligation.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateObligationStatusRequest.builder()
                                .status(ComplianceObligationStatus.FILED)
                                .notes("ITR-6 submitted on e-filing portal")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FILED"))
                .andExpect(jsonPath("$.data.filedDate").isNotEmpty());

        // 4. FILED -> COMPLETED
        mockMvc.perform(put("/api/v1/compliance/obligations/" + obligation.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateObligationStatusRequest.builder()
                                .status(ComplianceObligationStatus.COMPLETED)
                                .notes("ITR-V acknowledgement sent to client")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        // 5. Invalid transition: COMPLETED -> IN_PROGRESS should fail
        mockMvc.perform(put("/api/v1/compliance/obligations/" + obligation.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateObligationStatusRequest.builder()
                                .status(ComplianceObligationStatus.IN_PROGRESS)
                                .build())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Filter compliance calendar by location and assigned user")
    void testCalendarFiltering() throws Exception {
        LocationEntity branchLoc = LocationEntity.builder()
                .name("Pune Branch")
                .code("PUN-01")
                .city("Pune")
                .state("Maharashtra")
                .isHeadOffice(false)
                .isActive(true)
                .build();
        branchLoc.setOrganizationId(org.getId());
        branchLoc = locationRepository.save(branchLoc);

        ComplianceObligationEntity ob1 = ComplianceObligationEntity.builder()
                .clientId(client.getId())
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("GSTR-1 Mumbai")
                .periodLabel("August 2026")
                .statutoryDueDate(LocalDate.of(2026, 9, 11))
                .status(ComplianceObligationStatus.READY)
                .locationId(location.getId())
                .assignedUserId(adminUser.getId())
                .build();
        ob1.setOrganizationId(org.getId());
        obligationRepository.save(ob1);

        ComplianceObligationEntity ob2 = ComplianceObligationEntity.builder()
                .clientId(client.getId())
                .obligationType(ComplianceObligationType.TDS_RETURN)
                .title("TDS 26Q Pune")
                .periodLabel("Q2 2026-27")
                .statutoryDueDate(LocalDate.of(2026, 10, 31))
                .status(ComplianceObligationStatus.UPCOMING)
                .locationId(branchLoc.getId())
                .build();
        ob2.setOrganizationId(org.getId());
        obligationRepository.save(ob2);

        // Filter by Mumbai location
        mockMvc.perform(get("/api/v1/compliance/calendar")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("locationId", location.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("GSTR-1 Mumbai"));

        // Filter by assigned user
        mockMvc.perform(get("/api/v1/compliance/calendar")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("assignedUserId", adminUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("GSTR-1 Mumbai"));
    }
}
