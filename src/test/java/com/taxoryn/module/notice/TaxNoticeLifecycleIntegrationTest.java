package com.taxoryn.module.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.notice.dto.*;
import com.taxoryn.module.notice.entity.*;
import com.taxoryn.module.notice.enums.*;
import com.taxoryn.module.notice.repository.NoticeActivityRepository;
import com.taxoryn.module.notice.repository.NoticeHearingRepository;
import com.taxoryn.module.notice.repository.NoticeResponseRepository;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.dto.CreateWorkItemRequest;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.WorkItemEntity;
import com.taxoryn.module.task.model.WorkItemPriority;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.repository.WorkItemRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TaxNoticeLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaxNoticeRepository noticeRepository;

    @Autowired
    private NoticeResponseRepository responseRepository;

    @Autowired
    private NoticeHearingRepository hearingRepository;

    @Autowired
    private NoticeActivityRepository activityRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ComplianceObligationRepository obligationRepository;

    @Autowired
    private ComplianceWorkflowRepository workflowRepository;

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

    private OrganizationEntity organization;
    private LocationEntity location;
    private UserEntity adminUser;
    private UserEntity partnerUser;
    private EmployeeEntity employee;
    private ClientEntity client;
    private ComplianceWorkflowEntity workflow;
    private String adminToken;
    private String partnerToken;

    @BeforeEach
    void setUp() {
        cleanDb();

        organization = OrganizationEntity.builder()
                .name("Tax Notice Law Firm LLP")
                .email("admin@noticelaw.com")
                .status(OrganizationStatus.ACTIVE)
                .build();
        organization = organizationRepository.save(organization);

        TenantContext.setTenantId(organization.getId());

        location = LocationEntity.builder()
                .code("DEL-HQ")
                .name("Delhi Headquarters")
                .city("New Delhi")
                .state("Delhi")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        location = locationRepository.save(location);

        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .name("Organization Admin")
                .code("ORG_ADMIN")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity partnerRole = roleRepository.save(RoleEntity.builder()
                .name("Partner")
                .code("PARTNER")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        adminUser = userRepository.save(UserEntity.builder()
                .email("admin-" + UUID.randomUUID() + "@noticelaw.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("Senior")
                .lastName("Manager")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        partnerUser = userRepository.save(UserEntity.builder()
                .email("partner-" + UUID.randomUUID() + "@noticelaw.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("Senior")
                .lastName("Partner")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(partnerRole)))
                .build());

        employee = employeeRepository.save(EmployeeEntity.builder()
                .userId(adminUser.getId())
                .employeeCode("EMP-001")
                .firstName("Senior")
                .lastName("Advocate")
                .email(adminUser.getEmail())
                .status(EmployeeStatus.ACTIVE)
                .build());

        client = clientRepository.save(ClientEntity.builder()
                .displayName("Acme Global Industries Ltd")
                .legalName("Acme Global Industries Limited")
                .pan("ABCDE1234F")
                .clientType(ClientType.COMPANY)
                .locationId(location.getId())
                .status(ClientStatus.ACTIVE)
                .build());

        ComplianceObligationEntity obligation = obligationRepository.save(ComplianceObligationEntity.builder()
                .clientId(client.getId())
                .title("ITR Assessment Obligation")
                .obligationType(ComplianceObligationType.ITR_FILING)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .statutoryDueDate(LocalDate.now().plusDays(60))
                .status(ComplianceObligationStatus.UPCOMING)
                .build());

        workflow = workflowRepository.save(ComplianceWorkflowEntity.builder()
                .complianceObligationId(obligation.getId())
                .clientId(client.getId())
                .workflowStatus(ComplianceWorkflowStatus.IN_PROGRESS)
                .build());

        Set<String> perms = Set.of(
                "NOTICE_VIEW", "NOTICE_CREATE", "NOTICE_UPDATE", "NOTICE_DELETE",
                "NOTICE_RESPONSE_CREATE", "NOTICE_RESPONSE_REVIEW", "NOTICE_APPROVE",
                "NOTICE_SUBMIT", "NOTICE_CLOSE",
                "TASK_CREATE", "TASK_VIEW", "TASK_UPDATE"
        );
        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(adminUser.getId(), organization.getId(), adminUser.getEmail(), Set.of("ORG_ADMIN"), perms);
        partnerToken = "Bearer " + jwtTokenProvider.generateAccessToken(partnerUser.getId(), organization.getId(), partnerUser.getEmail(), Set.of("PARTNER"), perms);
    }

    @AfterEach
    void tearDown() {
        cleanDb();
        TenantContext.clear();
    }

    private void cleanDb() {
        TenantContext.clear();
        activityRepository.deleteAll();
        hearingRepository.deleteAll();
        responseRepository.deleteAll();
        workItemRepository.deleteAll();
        taskRepository.deleteAll();
        noticeRepository.deleteAll();
        workflowRepository.deleteAll();
        obligationRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete End-to-End Notice Lifecycle: Log -> Response Draft -> Checker Review -> Submission -> Hearing -> Resolution")
    void testCompleteNoticeLifecycleWorkflow() throws Exception {
        // Step 1: Log a new Tax Notice case
        CreateTaxNoticeRequest createRequest = CreateTaxNoticeRequest.builder()
                .clientId(client.getId())
                .locationId(location.getId())
                .workflowId(workflow.getId())
                .noticeNumber("IT-SCRUTINY-2026-" + UUID.randomUUID().toString().substring(0, 8))
                .dinNumber("DIN-2026-99887766")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("Scrutiny Notice u/s 143(2)")
                .section("143(2)")
                .subject("Notice for Scrutiny Assessment for AY 2025-26")
                .description("Verification of large cash transactions and business deduction claims.")
                .assessmentYear("2025-26")
                .financialYear("2024-25")
                .demandAmount(new BigDecimal("750000.00"))
                .noticeDate(LocalDate.now().minusDays(5))
                .receivedDate(LocalDate.now().minusDays(3))
                .responseDueDate(LocalDate.now().plusDays(25))
                .priority(NoticePriority.HIGH)
                .riskLevel(NoticeRisk.HIGH)
                .assignedEmployeeId(employee.getId())
                .reviewerEmployeeId(employee.getId())
                .partnerEmployeeId(employee.getId())
                .issuingAuthority("National Faceless Assessment Centre (NFAC)")
                .issuingOfficerName("Income Tax Officer Ward 1(1)")
                .createIntakeTask(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/notices")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.noticeNumber").value(createRequest.getNoticeNumber()))
                .andExpect(jsonPath("$.data.locationId").value(location.getId().toString()))
                .andExpect(jsonPath("$.data.locationName").value("Delhi Headquarters"))
                .andExpect(jsonPath("$.data.workflowId").value(workflow.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andExpect(jsonPath("$.data.responseStatus").value("REQUIRED"))
                .andReturn();

        TaxNoticeDto createdNotice = objectMapper.readValue(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").toString(),
                TaxNoticeDto.class);
        UUID noticeId = createdNotice.getId();

        // Step 2: Verify Notice Intake Task created & Link Work Item
        List<TaskEntity> tasks = taskRepository.findAllByOrganizationId(organization.getId());
        assertThat(tasks).isNotEmpty();

        CreateWorkItemRequest workItemRequest = CreateWorkItemRequest.builder()
                .clientId(client.getId())
                .locationId(location.getId())
                .noticeId(noticeId)
                .title("Draft response legal submissions for section 143(2)")
                .priority(WorkItemPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(10))
                .build();

        mockMvc.perform(post("/api/v1/work-items")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workItemRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.noticeId").value(noticeId.toString()));

        List<WorkItemEntity> linkedWorkItems = workItemRepository.findAllByOrganizationIdAndNoticeIdOrderByCreatedAtDesc(organization.getId(), noticeId);
        assertThat(linkedWorkItems).hasSize(1);
        assertThat(linkedWorkItems.get(0).getTitle()).contains("Draft response legal submissions");

        // Step 3: Maker drafts response
        CreateNoticeResponseRequest draftRequest = CreateNoticeResponseRequest.builder()
                .responseTitle("Detailed Reply & Ledger Submissions to Notice u/s 143(2)")
                .responseSummary("Reconciliation of bank credits and explanation of cash deposits with audited proofs.")
                .legalGrounds("Section 143(2) read with Section 68 and relevant ITAT decisions.")
                .factsOfCase("All bank deposits were made from declared sales already reflected in GSTR-1 and Books.")
                .submitForReview(false)
                .build();

        MvcResult draftResult = mockMvc.perform(post("/api/v1/notices/" + noticeId + "/responses")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.reviewStatus").value("DRAFT"))
                .andReturn();

        NoticeResponseDto responseDto = objectMapper.readValue(
                objectMapper.readTree(draftResult.getResponse().getContentAsString()).get("data").toString(),
                NoticeResponseDto.class);
        UUID responseId = responseDto.getId();

        // Step 4: Submit response for internal review
        mockMvc.perform(post("/api/v1/notices/" + noticeId + "/responses/" + responseId + "/submit-for-review")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("PENDING_REVIEW"));

        mockMvc.perform(get("/api/v1/notices/" + noticeId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INTERNAL_REVIEW"))
                .andExpect(jsonPath("$.data.responseStatus").value("UNDER_REVIEW"));

        // Step 5: Checker (Partner) approves response draft
        ReviewNoticeResponseRequest reviewRequest = ReviewNoticeResponseRequest.builder()
                .action("APPROVE_PARTNER")
                .comments("Draft arguments are strong and bank reconciliations verified. Approved for submission.")
                .build();

        mockMvc.perform(post("/api/v1/notices/" + noticeId + "/responses/" + responseId + "/review")
                        .header("Authorization", partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED_BY_PARTNER"));

        mockMvc.perform(get("/api/v1/notices/" + noticeId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PARTNER_APPROVED"))
                .andExpect(jsonPath("$.data.responseStatus").value("APPROVED"));

        // Step 6: Record official response submission on Tax Authority portal
        SubmitNoticeRequest submitRequest = SubmitNoticeRequest.builder()
                .submissionMode(SubmissionMode.INCOME_TAX_PORTAL)
                .portalAcknowledgementNumber("E-FILING-ACK-2026-889900")
                .remarks("Submitted on IT portal along with 4 annexures and CA certificate.")
                .build();

        mockMvc.perform(post("/api/v1/notices/" + noticeId + "/submit-response")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.responseStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.submissionReference").value("E-FILING-ACK-2026-889900"));

        // Step 7: Tax authority schedules Hearing / Video Conference
        ScheduleHearingRequest hearingRequest = ScheduleHearingRequest.builder()
                .hearingDate(LocalDate.now().plusDays(14))
                .hearingTime("11:30 AM")
                .hearingMode(HearingMode.VIDEO)
                .hearingLink("https://incometaxindia.webex.com/meet/nfac-01")
                .authorityName("National Faceless Assessment Centre")
                .officerName("Income Tax Officer Ward 1(1)")
                .proceedingsSummary("Preliminary video hearing on cash credit claims.")
                .designatedEmployeeId(employee.getId())
                .designatedPartnerId(employee.getId())
                .build();

        MvcResult hearingResult = mockMvc.perform(post("/api/v1/notices/" + noticeId + "/hearings")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hearingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.hearingMode").value("VIDEO"))
                .andReturn();

        NoticeHearingDto hearingDto = objectMapper.readValue(
                objectMapper.readTree(hearingResult.getResponse().getContentAsString()).get("data").toString(),
                NoticeHearingDto.class);
        UUID hearingId = hearingDto.getId();

        mockMvc.perform(get("/api/v1/notices/" + noticeId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HEARING_SCHEDULED"))
                .andExpect(jsonPath("$.data.hearingRequired").value(true));

        // Step 8: Record hearing completion outcome
        RecordHearingOutcomeRequest outcomeRequest = RecordHearingOutcomeRequest.builder()
                .status(HearingStatus.COMPLETED)
                .outcomeSummary("Hearing completed successfully. Assessing Officer satisfied with bank reconciliation proofs.")
                .build();

        mockMvc.perform(post("/api/v1/notices/" + noticeId + "/hearings/" + hearingId + "/outcome")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(outcomeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // Step 9: Resolve & Close Notice Case
        CloseNoticeRequest closeRequest = CloseNoticeRequest.builder()
                .closureStatus(NoticeStatus.RESOLVED)
                .closureRemarks("Assessment Order u/s 143(3) received with Nil demand additions. Case closed in favor of assessee.")
                .build();

        mockMvc.perform(post("/api/v1/notices/" + noticeId + "/close")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        // Step 10: Verify Activity Timeline
        mockMvc.perform(get("/api/v1/notices/" + noticeId + "/activities")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(5))));
    }
}
