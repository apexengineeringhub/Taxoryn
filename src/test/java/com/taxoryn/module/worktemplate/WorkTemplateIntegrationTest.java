package com.taxoryn.module.worktemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementPriority;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.service.model.ServiceStatus;
import com.taxoryn.module.service.repository.ServiceRepository;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.worktemplate.dto.CreateWorkTemplateRequest;
import com.taxoryn.module.worktemplate.dto.CreateWorkTemplateTaskRequest;
import com.taxoryn.module.worktemplate.dto.EnableEngagementTemplateRequest;
import com.taxoryn.module.worktemplate.dto.GenerateWorkInstanceRequest;
import com.taxoryn.module.worktemplate.dto.UpdateWorkInstanceStatusRequest;
import com.taxoryn.module.worktemplate.model.RecurrenceType;
import com.taxoryn.module.worktemplate.model.WorkInstanceStatus;
import com.taxoryn.module.worktemplate.model.WorkTemplateStatus;
import com.taxoryn.module.worktemplate.model.WorkTemplateType;
import com.taxoryn.module.worktemplate.repository.EngagementWorkTemplateRepository;
import com.taxoryn.module.worktemplate.repository.WorkInstanceRepository;
import com.taxoryn.module.worktemplate.repository.WorkTemplateRepository;
import com.taxoryn.module.worktemplate.repository.WorkTemplateTaskRepository;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WorkTemplateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private EngagementRepository engagementRepository;

    @Autowired
    private WorkTemplateRepository workTemplateRepository;

    @Autowired
    private WorkTemplateTaskRepository workTemplateTaskRepository;

    @Autowired
    private EngagementWorkTemplateRepository engagementWorkTemplateRepository;

    @Autowired
    private WorkInstanceRepository workInstanceRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private LocationEntity locA;
    private UserEntity userA;
    private UserEntity userB;
    private ClientEntity clientA;
    private ServiceEntity serviceGST;
    private ServiceEntity serviceTDS;
    private EngagementEntity engagementA;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        cleanUp();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        // Tenant A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Tax Advisory - " + UUID.randomUUID())
                .legalName("Alpha Tax Advisory LLP")
                .email("admin." + UUID.randomUUID() + "@alphatax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());

        LocationEntity loc = LocationEntity.builder()
                .name("Mumbai Central")
                .code("MUM-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Mumbai")
                .state("Maharashtra")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        loc.setOrganizationId(orgA.getId());
        locA = locationRepository.save(loc);

        userA = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("lead." + UUID.randomUUID() + "@alphatax.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Aditya")
                .lastName("Verma")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        ClientEntity client = ClientEntity.builder()
                .displayName("Acme Global Technologies")
                .clientType(ClientType.COMPANY)
                .pan("AABCN9999A")
                .locationId(locA.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        client.setOrganizationId(orgA.getId());
        clientA = clientRepository.save(client);

        serviceGST = serviceRepository.findByServiceCodeAndOrganizationIdIsNull("GST_COMPLIANCE")
                .orElseGet(() -> serviceRepository.save(ServiceEntity.builder()
                        .serviceCode("GST_COMPLIANCE")
                        .serviceName("GST Compliance & Returns")
                        .category(ServiceCategory.GST)
                        .status(ServiceStatus.ACTIVE)
                        .moduleCode("GST")
                        .build()));

        serviceTDS = serviceRepository.findByServiceCodeAndOrganizationIdIsNull("TDS_COMPLIANCE")
                .orElseGet(() -> serviceRepository.save(ServiceEntity.builder()
                        .serviceCode("TDS_COMPLIANCE")
                        .serviceName("TDS Compliance")
                        .category(ServiceCategory.TDS)
                        .status(ServiceStatus.ACTIVE)
                        .moduleCode("TDS")
                        .build()));

        EngagementEntity eng = EngagementEntity.builder()
                .clientId(clientA.getId())
                .serviceId(serviceGST.getId())
                .locationId(locA.getId())
                .engagementCode("ENG-2026-000001")
                .name("Acme Global - GST Compliance FY 2026-27")
                .status(EngagementStatus.ACTIVE)
                .priority(EngagementPriority.HIGH)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2027, 3, 31))
                .assignedUserId(userA.getId())
                .build();
        eng.setOrganizationId(orgA.getId());
        engagementA = engagementRepository.save(eng);

        tokenA = jwtTokenProvider.generateAccessToken(
                userA.getId(),
                orgA.getId(),
                userA.getEmail(),
                Set.of("ORG_ADMIN", "PRACTICE_ADMIN"),
                Set.of("CLIENT_CREATE", "CLIENT_WRITE", "CLIENT_VIEW", "CLIENT_UPDATE", "TASK_CREATE", "TASK_VIEW", "ORG_WRITE")
        );

        TenantContext.clear();

        // Tenant B (Isolation)
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Tax Solutions - " + UUID.randomUUID())
                .legalName("Beta Tax Solutions LLP")
                .email("admin." + UUID.randomUUID() + "@betatax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgB.getId());

        userB = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("admin." + UUID.randomUUID() + "@betatax.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Beta")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenB = jwtTokenProvider.generateAccessToken(
                userB.getId(),
                orgB.getId(),
                userB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "TASK_VIEW", "ORG_WRITE")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        taskRepository.deleteAll();
        workInstanceRepository.deleteAll();
        engagementWorkTemplateRepository.deleteAll();
        workTemplateTaskRepository.deleteAll();
        workTemplateRepository.deleteAll();
        engagementRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("End-to-End: Create Work Template with tasks, enable on Engagement, generate Work Instance, and enforce duplicate protection")
    void testFullWorkTemplateLifecycle() throws Exception {
        // 1. Create Work Template
        CreateWorkTemplateRequest createTplReq = CreateWorkTemplateRequest.builder()
                .serviceId(serviceGST.getId())
                .templateCode("GST_MONTHLY_CUSTOM")
                .name("Custom GST Monthly Compliance")
                .description("Practice standard GST filing template")
                .category(ServiceCategory.GST)
                .status(WorkTemplateStatus.ACTIVE)
                .templateType(WorkTemplateType.STATUTORY_COMPLIANCE)
                .recurrenceType(RecurrenceType.MONTHLY)
                .recurrenceInterval(1)
                .initialTasks(List.of(
                        CreateWorkTemplateTaskRequest.builder()
                                .name("Collect Sales & Purchase Invoices")
                                .sequenceOrder(1)
                                .relativeDueDays(5)
                                .defaultPriority(TaskPriority.MEDIUM)
                                .mandatory(true)
                                .build(),
                        CreateWorkTemplateTaskRequest.builder()
                                .name("GSTR-2B ITC Reconciliation")
                                .sequenceOrder(2)
                                .relativeDueDays(12)
                                .defaultPriority(TaskPriority.HIGH)
                                .mandatory(true)
                                .build(),
                        CreateWorkTemplateTaskRequest.builder()
                                .name("File GSTR-3B Return")
                                .sequenceOrder(3)
                                .relativeDueDays(20)
                                .defaultPriority(TaskPriority.URGENT)
                                .mandatory(true)
                                .build()
                ))
                .build();

        String createTplResponse = mockMvc.perform(post("/api/v1/work-templates")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTplReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Custom GST Monthly Compliance"))
                .andExpect(jsonPath("$.data.taskCount").value(3))
                .andReturn().getResponse().getContentAsString();

        String templateIdStr = objectMapper.readTree(createTplResponse).path("data").path("id").asText();
        UUID templateId = UUID.fromString(templateIdStr);

        // 2. List Work Templates
        mockMvc.perform(get("/api/v1/work-templates")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());

        // 3. Enable Work Template on Engagement
        EnableEngagementTemplateRequest enableReq = EnableEngagementTemplateRequest.builder()
                .recurrenceType(RecurrenceType.MONTHLY)
                .recurrenceInterval(1)
                .build();

        mockMvc.perform(post("/api/v1/engagements/{engagementId}/work-templates/{templateId}/enable", engagementA.getId(), templateId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enableReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(true));

        // 4. Generate Work Instance for April 2026
        LocalDate periodStart = LocalDate.of(2026, 4, 1);
        LocalDate periodEnd = LocalDate.of(2026, 4, 30);
        GenerateWorkInstanceRequest genReq = GenerateWorkInstanceRequest.builder()
                .templateId(templateId)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .dueDate(LocalDate.of(2026, 5, 20))
                .build();

        String genResponse = mockMvc.perform(post("/api/v1/engagements/{engagementId}/work/generate", engagementA.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Custom GST Monthly Compliance — April 2026"))
                .andExpect(jsonPath("$.data.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.data.totalTasks").value(3))
                .andExpect(jsonPath("$.data.tasks[0].title").value("Collect Sales & Purchase Invoices — April 2026"))
                .andExpect(jsonPath("$.data.tasks[0].dueDate").value("2026-04-06")) // periodStart (4/1) + 5 days = 4/6
                .andReturn().getResponse().getContentAsString();

        String workInstanceIdStr = objectMapper.readTree(genResponse).path("data").path("id").asText();
        UUID workInstanceId = UUID.fromString(workInstanceIdStr);

        // 5. CRITICAL: Duplicate Work Protection (Requirement 11)
        mockMvc.perform(post("/api/v1/engagements/{engagementId}/work/generate", engagementA.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists for this engagement")));

        // 6. Get Work Instance Details
        mockMvc.perform(get("/api/v1/work-instances/{id}", workInstanceId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(workInstanceId.toString()))
                .andExpect(jsonPath("$.data.totalTasks").value(3));

        // 7. Update Work Instance Status
        UpdateWorkInstanceStatusRequest statusReq = UpdateWorkInstanceStatusRequest.builder()
                .status(WorkInstanceStatus.IN_PROGRESS)
                .notes("Started filing preparation")
                .build();

        mockMvc.perform(patch("/api/v1/work-instances/{id}/status", workInstanceId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        // 8. Multi-Tenant Isolation Check: Tenant B cannot access Tenant A's work template or instance
        mockMvc.perform(get("/api/v1/work-templates/{id}", templateId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/work-instances/{id}", workInstanceId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Enforce Requirement 22: Mismatched template service cannot be enabled on engagement")
    void testMismatchedServiceTemplateEnablementFails() throws Exception {
        // Create TDS Template
        CreateWorkTemplateRequest tdsTplReq = CreateWorkTemplateRequest.builder()
                .serviceId(serviceTDS.getId())
                .templateCode("TDS_QTR_CUSTOM")
                .name("Quarterly TDS Compliance")
                .category(ServiceCategory.TDS)
                .status(WorkTemplateStatus.ACTIVE)
                .recurrenceType(RecurrenceType.QUARTERLY)
                .build();

        String createTplResponse = mockMvc.perform(post("/api/v1/work-templates")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tdsTplReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String tdsTplId = objectMapper.readTree(createTplResponse).path("data").path("id").asText();

        // Try to attach TDS Template to GST Engagement -> Must Fail
        mockMvc.perform(post("/api/v1/engagements/{engagementId}/work-templates/{templateId}/enable", engagementA.getId(), tdsTplId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("TEMPLATE_SERVICE_MISMATCH")));
    }
}
