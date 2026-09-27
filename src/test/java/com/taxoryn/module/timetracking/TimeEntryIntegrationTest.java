package com.taxoryn.module.timetracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.entity.WorkItemEntity;
import com.taxoryn.module.task.model.WorkItemStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.repository.WorkItemRepository;
import com.taxoryn.module.timetracking.dto.CreateTimeEntryRequest;
import com.taxoryn.module.timetracking.dto.UpdateTimeEntryRequest;
import com.taxoryn.module.timetracking.model.TimeEntryStatus;
import com.taxoryn.module.timetracking.repository.TimeEntryRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TimeEntryIntegrationTest {

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
    private EngagementRepository engagementRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org;
    private LocationEntity loc;
    private UserEntity user;
    private ClientEntity client;
    private EngagementEntity engagement;
    private WorkItemEntity workItem;
    private TaskEntity task;
    private String token;

    @BeforeEach
    void setUp() {
        cleanUp();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        org = organizationRepository.save(OrganizationEntity.builder()
                .name("Mehta & Co - " + UUID.randomUUID())
                .legalName("Mehta & Co LLP")
                .email("contact." + UUID.randomUUID() + "@mehta.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        LocationEntity location = LocationEntity.builder()
                .name("Ahmedabad HQ")
                .code("AMD-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Ahmedabad")
                .state("Gujarat")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        location.setOrganizationId(org.getId());
        loc = locationRepository.save(location);

        user = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("lead." + UUID.randomUUID() + "@mehta.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Suresh")
                .lastName("Mehta")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        token = jwtTokenProvider.generateAccessToken(
                user.getId(),
                org.getId(),
                user.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "TASK_VIEW", "TASK_READ", "TASK_CREATE", "TASK_WRITE", "TASK_UPDATE", "CLIENT_VIEW")
        );

        ClientEntity clientEntity = ClientEntity.builder()
                .displayName("Adani Enterprises Ltd")
                .legalName("Adani Enterprises Limited")
                .clientType(ClientType.COMPANY)
                .pan("AABCA9999A")
                .locationId(loc.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        clientEntity.setOrganizationId(org.getId());
        client = clientRepository.save(clientEntity);

        EngagementEntity eng = EngagementEntity.builder()
                .locationId(loc.getId())
                .clientId(client.getId())
                .name("Tax Audit Sec 44AB FY 2026-27")
                .build();
        eng.setOrganizationId(org.getId());
        engagement = engagementRepository.save(eng);

        WorkItemEntity wi = WorkItemEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .title("Draft 3CD Annexures")
                .status(WorkItemStatus.IN_PROGRESS)
                .build();
        wi.setOrganizationId(org.getId());
        workItem = workItemRepository.save(wi);

        TaskEntity t = TaskEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .workItemId(workItem.getId())
                .title("Verify 43B disallowances")
                .status(TaskStatus.IN_PROGRESS)
                .build();
        t.setOrganizationId(org.getId());
        task = taskRepository.save(t);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        timeEntryRepository.deleteAll();
        taskRepository.deleteAll();
        workItemRepository.deleteAll();
        engagementRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Create Time Entry linked to Client, Engagement, Work Item, and Task, update details, and delete")
    void testCreateGetUpdateTimeEntry() throws Exception {
        CreateTimeEntryRequest request = CreateTimeEntryRequest.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .engagementId(engagement.getId())
                .workItemId(workItem.getId())
                .taskId(task.getId())
                .userId(user.getId())
                .entryDate(LocalDate.of(2026, 8, 10))
                .durationMinutes(150)
                .description("Detailed audit of clause 21(a) disallowances and reconciliation with ledger")
                .billable(true)
                .billingRate(new BigDecimal("2500.00"))
                .status(TimeEntryStatus.SUBMITTED)
                .build();

        String response = mockMvc.perform(post("/api/v1/time-entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.durationMinutes").value(150))
                .andExpect(jsonPath("$.data.billable").value(true))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andReturn().getResponse().getContentAsString();

        UUID timeEntryId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // Get by ID
        mockMvc.perform(get("/api/v1/time-entries/" + timeEntryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientName").value("Adani Enterprises Ltd"))
                .andExpect(jsonPath("$.data.engagementName").value("Tax Audit Sec 44AB FY 2026-27"))
                .andExpect(jsonPath("$.data.workItemTitle").value("Draft 3CD Annexures"))
                .andExpect(jsonPath("$.data.taskTitle").value("Verify 43B disallowances"))
                .andExpect(jsonPath("$.data.userName").value(user.getFullName()));

        // Update Time Entry
        UpdateTimeEntryRequest updateReq = UpdateTimeEntryRequest.builder()
                .durationMinutes(180)
                .status(TimeEntryStatus.APPROVED)
                .description("Finalized audit verification after partner review")
                .build();

        mockMvc.perform(put("/api/v1/time-entries/" + timeEntryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.durationMinutes").value(180))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // Delete Time Entry
        mockMvc.perform(delete("/api/v1/time-entries/" + timeEntryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Verify Not Found
        mockMvc.perform(get("/api/v1/time-entries/" + timeEntryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Retrieve Time Entries by Client ID for Client 360")
    void testGetTimeEntriesByClientId() throws Exception {
        CreateTimeEntryRequest req1 = CreateTimeEntryRequest.builder()
                .clientId(client.getId())
                .entryDate(LocalDate.of(2026, 8, 1))
                .durationMinutes(60)
                .description("Consulting session 1")
                .build();

        CreateTimeEntryRequest req2 = CreateTimeEntryRequest.builder()
                .clientId(client.getId())
                .entryDate(LocalDate.of(2026, 8, 2))
                .durationMinutes(90)
                .description("Consulting session 2")
                .build();

        mockMvc.perform(post("/api/v1/time-entries").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/time-entries").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/time-entries/clients/" + client.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
