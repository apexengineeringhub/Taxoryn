package com.taxoryn.module.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
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
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.repository.TaskRepository;
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
class TaxNoticeManagementIntegrationTest {

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
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

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

    private OrganizationEntity org1;
    private OrganizationEntity org2;

    private UserEntity preparerUser;
    private UserEntity reviewerUser;
    private UserEntity partnerUser;
    private UserEntity clientPortalUser;
    private UserEntity org2AdminUser;

    private String preparerToken;
    private String reviewerToken;
    private String partnerToken;
    private String clientPortalToken;
    private String org2AdminToken;

    private EmployeeEntity employeePreparer;
    private EmployeeEntity employeeReviewer;
    private EmployeeEntity employeePartner;

    private ClientEntity client1;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        activityRepository.deleteAll();
        hearingRepository.deleteAll();
        responseRepository.deleteAll();
        noticeRepository.deleteAll();
        taskRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organization 1 & 2
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA Associates")
                .email("admin@apexca.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Horizon Tax Consultants")
                .email("admin@horizontax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Setup Roles
        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity clientRole = roleRepository.save(RoleEntity.builder()
                .code("CLIENT_USER")
                .name("Client Portal User")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        Set<String> allNoticePermissions = Set.of(
                "NOTICE_VIEW", "NOTICE_CREATE", "NOTICE_EDIT", "NOTICE_DELETE",
                "NOTICE_ASSIGN", "NOTICE_DRAFT", "NOTICE_REVIEW", "NOTICE_APPROVE",
                "NOTICE_HEARING_MANAGE", "NOTICE_FILE", "NOTICE_CLOSE",
                "TASK_CREATE", "TASK_VIEW", "TASK_UPDATE"
        );

        TenantContext.setTenantId(org1.getId());

        // 3. Create Users
        preparerUser = userRepository.save(UserEntity.builder()
                .email("preparer@apexca.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Aakash")
                .lastName("Verma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        reviewerUser = userRepository.save(UserEntity.builder()
                .email("reviewer@apexca.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Sunil")
                .lastName("Mehta")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        partnerUser = userRepository.save(UserEntity.builder()
                .email("partner@apexca.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Praveen")
                .lastName("Singhal")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        clientPortalUser = userRepository.save(UserEntity.builder()
                .email("contact@vedantasolutions.com")
                .passwordHash(passwordEncoder.encode("ClientPass123!"))
                .firstName("Vedanta")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientRole)))
                .build());

        // 4. Create Employees
        employeePreparer = employeeRepository.save(EmployeeEntity.builder()
                .employeeCode("EMP-01")
                .userId(preparerUser.getId())
                .firstName("Aakash")
                .lastName("Verma")
                .email("preparer@apexca.com")
                .department("Direct Tax")
                .designation("Tax Associate")
                .status(EmployeeStatus.ACTIVE)
                .build());

        employeeReviewer = employeeRepository.save(EmployeeEntity.builder()
                .employeeCode("EMP-02")
                .userId(reviewerUser.getId())
                .firstName("Sunil")
                .lastName("Mehta")
                .email("reviewer@apexca.com")
                .department("Direct Tax")
                .designation("Senior Manager")
                .status(EmployeeStatus.ACTIVE)
                .build());

        employeePartner = employeeRepository.save(EmployeeEntity.builder()
                .employeeCode("EMP-03")
                .userId(partnerUser.getId())
                .firstName("Praveen")
                .lastName("Singhal")
                .email("partner@apexca.com")
                .department("Direct Tax")
                .designation("Partner")
                .status(EmployeeStatus.ACTIVE)
                .build());

        // 5. Create Client
        client1 = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Vedanta Solutions Pvt Ltd")
                .legalName("Vedanta Solutions Private Limited")
                .pan("AABCV1234D")
                .gstin("27AABCV1234D1Z5")
                .email("contact@vedantasolutions.com")
                .assignedEmployeeId(employeePreparer.getId())
                .status(ClientStatus.ACTIVE)
                .build());

        clientPortalUser.setClientId(client1.getId());
        userRepository.save(clientPortalUser);

        // 6. Setup Org2 Admin
        TenantContext.setTenantId(org2.getId());
        org2AdminUser = userRepository.save(UserEntity.builder()
                .email("admin@horizontax.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Hitesh")
                .lastName("Shah")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        // 7. JWT Tokens
        preparerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                preparerUser.getId(), org1.getId(), preparerUser.getEmail(), Set.of("ORG_ADMIN"), allNoticePermissions);

        reviewerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                reviewerUser.getId(), org1.getId(), reviewerUser.getEmail(), Set.of("ORG_ADMIN"), allNoticePermissions);

        partnerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                partnerUser.getId(), org1.getId(), partnerUser.getEmail(), Set.of("ORG_ADMIN"), allNoticePermissions);

        clientPortalToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                clientPortalUser.getId(), org1.getId(), client1.getId(), clientPortalUser.getEmail(), Set.of("CLIENT_USER"), Set.of("NOTICE_VIEW", "CLIENT_PORTAL_ACCESS"));

        org2AdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                org2AdminUser.getId(), org2.getId(), org2AdminUser.getEmail(), Set.of("ORG_ADMIN"), allNoticePermissions);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Complete End-to-End Tax Notice Lifecycle: Intake -> Maker-Checker -> Hearing -> Portal Submission -> Case Closure")
    void testEndToEndTaxNoticeLifecycle() throws Exception {
        // --- 1. CASE INTAKE / LOGGING ---
        CreateTaxNoticeRequest createRequest = CreateTaxNoticeRequest.builder()
                .clientId(client1.getId())
                .noticeNumber("ITBA/AST/S/143(1)/2024-25/1062839201(1)")
                .dinNumber("ITBA/AST/S/143(1)/2024-25/1062839201(1)")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("143(1) - Summary Intimation")
                .section("143(1)")
                .subject("Intimation u/s 143(1) with Disallowance of Chapter VI-A Deductions")
                .description("Intimation proposing total tax demand of Rs. 4,50,000 for AY 2024-25 due to mismatch in 80JJAA")
                .assessmentYear("2024-25")
                .financialYear("2023-24")
                .demandAmount(new BigDecimal("450000.00"))
                .receivedDate(LocalDate.now().minusDays(2))
                .responseDueDate(LocalDate.now().plusDays(10))
                .assignedEmployeeId(employeePreparer.getId())
                .reviewerEmployeeId(employeeReviewer.getId())
                .partnerEmployeeId(employeePartner.getId())
                .issuingAuthority("Income Tax Department, CPC Bengaluru")
                .issuingOfficerName("Assessing Officer Ward 3(1)")
                .internalNotes("High probability of rectification u/s 154 based on Form 10DA audit report.")
                .createIntakeTask(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/notices")
                        .header("Authorization", preparerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.noticeNumber").value("ITBA/AST/S/143(1)/2024-25/1062839201(1)"))
                .andExpect(jsonPath("$.data.department").value("INCOME_TAX"))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andExpect(jsonPath("$.data.demandAmount").value(450000.00))
                .andExpect(jsonPath("$.data.clientName").value("Vedanta Solutions Pvt Ltd"))
                .andExpect(jsonPath("$.data.assignedEmployeeName").value("Aakash Verma"))
                .andExpect(jsonPath("$.data.reviewerEmployeeName").value("Sunil Mehta"))
                .andExpect(jsonPath("$.data.partnerEmployeeName").value("Praveen Singhal"))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String noticeId = objectMapper.readTree(responseBody).path("data").path("id").asText();
        assertThat(noticeId).isNotBlank();

        // Verify intake task was automatically created in database
        TenantContext.setTenantId(org1.getId());
        var tasks = taskRepository.findAllByOrganizationIdAndNoticeId(org1.getId(), UUID.fromString(noticeId));
        assertThat(tasks).isNotEmpty();
        assertThat(tasks.get(0).getTitle()).contains("Notice Response Prep");
        TenantContext.clear();

        // --- 2. MAKER-CHECKER RESPONSE WORKFLOW ---

        // 2a. Maker creates response draft v1
        CreateNoticeResponseRequest draftRequest = CreateNoticeResponseRequest.builder()
                .responseTitle("Written Submission & Rectification u/s 154 against Intimation u/s 143(1)")
                .responseSummary("Detailed explanation of Form 10DA filed prior to return due date.")
                .factsOfCase("The assessee filed Return of Income on 31-10-2024 with claim of Deduction u/s 80JJAA.")
                .legalGrounds("Section 80JJAA(2)(c) read with Rule 19AB compliance demonstrated via CA audit certificate.")
                .submitForReview(true) // Submit directly for review
                .build();

        MvcResult draftResult = mockMvc.perform(post("/api/v1/notices/" + noticeId + "/responses")
                        .header("Authorization", preparerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.reviewStatus").value("PENDING_REVIEW"))
                .andReturn();

        String responseId = objectMapper.readTree(draftResult.getResponse().getContentAsString()).path("data").path("id").asText();

        // Verify notice status moved to INTERNAL_REVIEW
        mockMvc.perform(get("/api/v1/notices/" + noticeId)
                        .header("Authorization", preparerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INTERNAL_REVIEW"));

        // 2b. Maker self-review prevention (Maker cannot approve their own draft)
        ReviewNoticeResponseRequest selfReviewRequest = ReviewNoticeResponseRequest.builder()
                .action("APPROVE_REVIEW")
                .comments("Self approval attempt")
                .build();

        mockMvc.perform(post("/api/v1/notices/" + noticeId + "/responses/" + responseId + "/review")
                        .header("Authorization", preparerToken) // Preparer trying to review own draft
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfReviewRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("Maker-Checker Violation")));

        // 2c. Checker reviews and approves draft
        ReviewNoticeResponseRequest checkerApprove = ReviewNoticeResponseRequest.builder()
                .action("APPROVE_REVIEW")
                .comments("Facts and statutory grounds verified against ITAT Chennai bench precedents.")
                .build();

        mockMvc.perform(post("/api/v1/notices/" + noticeId + "/responses/" + responseId + "/review")
                        .header("Authorization", reviewerToken) // Sunil Mehta
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkerApprove)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED_BY_REVIEWER"))
                .andExpect(jsonPath("$.data.reviewedByUserName").value("Sunil Mehta"));

        // 2d. Partner final sign-off
        ReviewNoticeResponseRequest partnerApprove = ReviewNoticeResponseRequest.builder()
                .action("APPROVE_PARTNER")
                .comments("Approved for upload on Income Tax e-Filing portal.")
                .build();

        mockMvc.perform(post("/api/v1/notices/" + noticeId + "/responses/" + responseId + "/review")
                        .header("Authorization", partnerToken) // Praveen Singhal
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partnerApprove)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED_BY_PARTNER"))
                .andExpect(jsonPath("$.data.approvedByUserName").value("Praveen Singhal"));

        // Verify notice status transitioned to PARTNER_APPROVED
        mockMvc.perform(get("/api/v1/notices/" + noticeId)
                        .header("Authorization", preparerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PARTNER_APPROVED"));

        // --- 3. SCHEDULE HEARING ---
        ScheduleHearingRequest hearingRequest = ScheduleHearingRequest.builder()
                .hearingDate(LocalDate.now().plusDays(5))
                .hearingTime("11:30 AM")
                .hearingMode(HearingMode.VIRTUAL_VC)
                .hearingLink("https://incometax.webex.com/meet/ao-ward31")
                .authorityName("Income Tax Appellate / Faceless Assessment Center")
                .officerName("Shri R. K. Sharma, Addl. CIT")
                .proceedingsSummary("Faceless video conferencing hearing scheduled for oral arguments on 80JJAA deduction.")
                .build();

        MvcResult hearingResult = mockMvc.perform(post("/api/v1/notices/" + noticeId + "/hearings")
                        .header("Authorization", preparerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hearingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.hearingMode").value("VIRTUAL_VC"))
                .andExpect(jsonPath("$.data.hearingLink").value("https://incometax.webex.com/meet/ao-ward31"))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andReturn();

        String hearingId = objectMapper.readTree(hearingResult.getResponse().getContentAsString()).path("data").path("id").asText();

        // 3b. Record Hearing Outcome
        RecordHearingOutcomeRequest outcomeRequest = RecordHearingOutcomeRequest.builder()
                .status(HearingStatus.COMPLETED)
                .proceedingsSummary("Argued all aspects before Addl CIT; produced CA Certificate and bank statement extracts.")
                .outcomeSummary("Assessing Officer was satisfied; directed to submit formal verification on portal.")
                .nextAction("File written reply on e-filing portal")
                .build();

        mockMvc.perform(put("/api/v1/notices/" + noticeId + "/hearings/" + hearingId + "/outcome")
                        .header("Authorization", preparerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(outcomeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.outcomeSummary").value(containsString("Assessing Officer was satisfied")));

        // --- 4. RECORD PORTAL FILING ---
        SubmitNoticeRequest submitRequest = SubmitNoticeRequest.builder()
                .submissionMode(SubmissionMode.INCOME_TAX_PORTAL)
                .portalAcknowledgementNumber("ITBA-ACK-2024-981726")
                .remarks("Written response along with Form 10DA submitted on IT e-filing portal.")
                .build();

        mockMvc.perform(post("/api/v1/notices/" + noticeId + "/submit")
                        .header("Authorization", preparerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.portalAcknowledgementNumber").value("ITBA-ACK-2024-981726"))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty());

        // --- 5. RESOLUTION & CLOSURE ---
        CloseNoticeRequest closeRequest = CloseNoticeRequest.builder()
                .closureStatus(NoticeStatus.DEMAND_DROPPED)
                .closureDate(LocalDate.now())
                .closureRemarks("Rectification order passed u/s 154; total demand reduced to NIL. Refund issued.")
                .build();

        mockMvc.perform(post("/api/v1/notices/" + noticeId + "/close")
                        .header("Authorization", partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DEMAND_DROPPED"))
                .andExpect(jsonPath("$.data.closureRemarks").value(containsString("total demand reduced to NIL")));

        // --- 6. AUDIT TRAIL VERIFICATION ---
        mockMvc.perform(get("/api/v1/notices/" + noticeId + "/activities")
                        .header("Authorization", preparerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(5))));

        // --- 7. DASHBOARD STATS VERIFICATION ---
        mockMvc.perform(get("/api/v1/notices/dashboard/stats")
                        .header("Authorization", preparerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalActiveNotices").value(0))
                .andExpect(jsonPath("$.data.byStatus.DEMAND_DROPPED").value(1));
    }

    @Test
    @DisplayName("Client Portal Isolation: Client cannot see internal notes, reviewer comments, or partner sign-offs")
    void testClientPortalSanitization() throws Exception {
        TenantContext.setTenantId(org1.getId());
        TaxNoticeEntity notice = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(client1.getId())
                .noticeNumber("GST-SCN-2024-88192")
                .department(NoticeDepartment.GST)
                .noticeType("DRC-01 Show Cause Notice")
                .section("Section 73")
                .subject("ITC Mismatch between GSTR-2B and GSTR-3B")
                .status(NoticeStatus.UNDER_REVIEW)
                .priority(NoticePriority.HIGH)
                .demandAmount(new BigDecimal("120000.00"))
                .receivedDate(LocalDate.now().minusDays(1))
                .responseDueDate(LocalDate.now().plusDays(15))
                .internalNotes("High client risk: supplier GSTR-1 not filed for Nov 2023.")
                .assignedEmployeeId(employeePreparer.getId())
                .reviewerEmployeeId(employeeReviewer.getId())
                .build());
        TenantContext.clear();

        // 1. Practice view (shows internal notes)
        mockMvc.perform(get("/api/v1/notices/" + notice.getId())
                        .header("Authorization", preparerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.internalNotes").value("High client risk: supplier GSTR-1 not filed for Nov 2023."));

        // 2. Client Portal view (must NOT expose internal notes or risk metadata)
        mockMvc.perform(get("/api/v1/portal/notices")
                        .header("Authorization", clientPortalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].noticeNumber").value("GST-SCN-2024-88192"))
                .andExpect(jsonPath("$.data.content[0].department").value("GST"))
                .andExpect(jsonPath("$.data.content[0].internalNotes").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].reviewerEmployeeName").doesNotExist());
    }

    @Test
    @DisplayName("Multi-Tenant Isolation: Notice created in Org1 is inaccessible by Org2")
    void testMultiTenantIsolation() throws Exception {
        TenantContext.setTenantId(org1.getId());
        TaxNoticeEntity noticeOrg1 = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(client1.getId())
                .noticeNumber("TDS-DEMAND-2024-11")
                .department(NoticeDepartment.TDS)
                .noticeType("TRACES Short Deduction Intimation")
                .section("Section 201(1A)")
                .subject("Late Payment Interest Demand")
                .status(NoticeStatus.RECEIVED)
                .priority(NoticePriority.MEDIUM)
                .demandAmount(new BigDecimal("15000.00"))
                .receivedDate(LocalDate.now().minusDays(2))
                .responseDueDate(LocalDate.now().plusDays(20))
                .build());
        TenantContext.clear();

        // Org2 Admin trying to access Org1 notice directly
        mockMvc.perform(get("/api/v1/notices/" + noticeOrg1.getId())
                        .header("Authorization", org2AdminToken))
                .andExpect(status().isNotFound());

        // Org2 list notices returns empty page
        mockMvc.perform(get("/api/v1/notices")
                        .header("Authorization", org2AdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }
}
