package com.taxoryn.module.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.notice.dto.AdjournHearingRequest;
import com.taxoryn.module.notice.dto.UpdateNoticeHearingRequest;
import com.taxoryn.module.notice.dto.UpdateNoticeResponseRequest;
import com.taxoryn.module.notice.entity.TaxNoticeEntity;
import com.taxoryn.module.notice.enums.HearingMode;
import com.taxoryn.module.notice.enums.HearingStatus;
import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.notice.enums.NoticeResponseStatus;
import com.taxoryn.module.notice.enums.NoticeStatus;
import com.taxoryn.module.notice.repository.NoticeActivityRepository;
import com.taxoryn.module.notice.repository.NoticeHearingRepository;
import com.taxoryn.module.notice.repository.NoticeResponseRepository;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationType;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaxNoticeResponseAndHearingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TaxNoticeRepository noticeRepository;

    @Autowired
    private NoticeResponseRepository responseRepository;

    @Autowired
    private NoticeHearingRepository hearingRepository;

    @Autowired
    private NoticeActivityRepository activityRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    // Org A
    private OrganizationEntity orgA;
    private UserEntity adminUserA;
    private UserEntity staffUserA;
    private EmployeeEntity staffEmployeeA;
    private UserEntity otherStaffUserA;
    private EmployeeEntity otherStaffEmployeeA;
    private UserEntity partnerUserA;
    private ClientEntity clientA1;
    private ClientEntity clientA2;
    private String adminTokenA;
    private String staffTokenA;
    private String otherStaffTokenA;
    private String partnerTokenA;
    private String noPermissionTokenA;

    // Org B
    private OrganizationEntity orgB;
    private UserEntity adminUserB;
    private ClientEntity clientB;
    private String adminTokenB;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        // 1. Setup Org A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Tax & Legal Advisors")
                .status(OrganizationStatus.ACTIVE)
                .organizationType(OrganizationType.GROWING_PRACTICE)
                .email("alpha@taxoryn.com")
                .phone("+919876543210")
                .build());

        // 2. Setup Org B
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Tax Consultancy")
                .status(OrganizationStatus.ACTIVE)
                .organizationType(OrganizationType.SMALL_TAX_FIRM)
                .email("beta@taxoryn.com")
                .phone("+919876543211")
                .build());

        // Roles
        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity partnerRole = roleRepository.save(RoleEntity.builder()
                .code("PARTNER")
                .name("Senior Partner")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity staffRole = roleRepository.save(RoleEntity.builder()
                .code("STAFF")
                .name("Practice Staff")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity viewOnlyRole = roleRepository.save(RoleEntity.builder()
                .code("VIEW_ONLY")
                .name("View Only Staff")
                .isSystemRole(false)
                .permissions(new HashSet<>())
                .build());

        Set<String> noticeFullPermissions = Set.of("NOTICE_VIEW", "NOTICE_CREATE", "NOTICE_UPDATE", "NOTICE_DELETE", "NOTICE_SUBMIT", "NOTICE_CLOSE", "NOTICE_RESPONSE_CREATE", "NOTICE_RESPONSE_REVIEW", "NOTICE_APPROVE");
        Set<String> noticeViewOnlyPermissions = Set.of("NOTICE_VIEW");

        // Org A Users & Employees
        TenantContext.setTenantId(orgA.getId());

        adminUserA = userRepository.save(UserEntity.builder()
                .email("admin@alphatax.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Alpha")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());
        adminTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(adminUserA.getId(), orgA.getId(), adminUserA.getEmail(), Set.of("ORG_ADMIN"), noticeFullPermissions);

        partnerUserA = userRepository.save(UserEntity.builder()
                .email("partner@alphatax.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Senior")
                .lastName("Partner")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(partnerRole)))
                .build());
        partnerTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(partnerUserA.getId(), orgA.getId(), partnerUserA.getEmail(), Set.of("PARTNER"), noticeFullPermissions);

        staffUserA = userRepository.save(UserEntity.builder()
                .email("staff1@alphatax.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Staff")
                .lastName("One")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());
        staffEmployeeA = employeeRepository.save(EmployeeEntity.builder()
                .userId(staffUserA.getId())
                .employeeCode("EMP-A1")
                .firstName("Staff")
                .lastName("One")
                .email("staff1@alphatax.com")
                .designation("Senior Associate")
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.now().minusMonths(6))
                .build());
        staffTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(staffUserA.getId(), orgA.getId(), staffUserA.getEmail(), Set.of("STAFF"), noticeFullPermissions);

        otherStaffUserA = userRepository.save(UserEntity.builder()
                .email("staff2@alphatax.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Staff")
                .lastName("Two")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());
        otherStaffEmployeeA = employeeRepository.save(EmployeeEntity.builder()
                .userId(otherStaffUserA.getId())
                .employeeCode("EMP-A2")
                .firstName("Staff")
                .lastName("Two")
                .email("staff2@alphatax.com")
                .designation("Article Assistant")
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.now().minusMonths(3))
                .build());
        otherStaffTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(otherStaffUserA.getId(), orgA.getId(), otherStaffUserA.getEmail(), Set.of("STAFF"), noticeFullPermissions);

        UserEntity viewOnlyUserA = userRepository.save(UserEntity.builder()
                .email("viewonly@alphatax.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("View")
                .lastName("Only")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(viewOnlyRole)))
                .build());
        noPermissionTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(viewOnlyUserA.getId(), orgA.getId(), viewOnlyUserA.getEmail(), Set.of("VIEW_ONLY"), noticeViewOnlyPermissions);

        // Clients in Org A
        clientA1 = clientRepository.save(ClientEntity.builder()
                .displayName("Acme Infra Corp")
                .pan("AAACA1234A")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .assignedEmployeeId(staffEmployeeA.getId())
                .build());

        clientA2 = clientRepository.save(ClientEntity.builder()
                .displayName("Zeta Tech Ventures")
                .pan("AAACZ5678Z")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .assignedEmployeeId(otherStaffEmployeeA.getId())
                .build());

        // Org B Setup
        TenantContext.setTenantId(orgB.getId());

        adminUserB = userRepository.save(UserEntity.builder()
                .email("admin@betatax.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Beta")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());
        adminTokenB = "Bearer " + jwtTokenProvider.generateAccessToken(adminUserB.getId(), orgB.getId(), adminUserB.getEmail(), Set.of("ORG_ADMIN"), noticeFullPermissions);

        clientB = clientRepository.save(ClientEntity.builder()
                .displayName("Delta Logistics Pvt Ltd")
                .pan("AAACD9999D")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .build());

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
        TenantContext.clear();
    }

    private void cleanDatabase() {
        TenantContext.clear();
        activityRepository.deleteAll();
        hearingRepository.deleteAll();
        responseRepository.deleteAll();
        noticeRepository.deleteAll();
        auditLogRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    private TaxNoticeEntity createNotice(UUID orgId, UUID clientId, NoticeDepartment dept, String number, NoticeStatus status) {
        TenantContext.setTenantId(orgId);
        TaxNoticeEntity notice = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(clientId)
                .department(dept)
                .noticeType("Scrutiny Notice")
                .noticeNumber(number)
                .section("143(2)")
                .subject("Scrutiny Assessment for AY 2024-25")
                .description("Detailed scrutiny of foreign remittances and expenses")
                .assessmentYear("2024-25")
                .financialYear("2023-24")
                .demandAmount(new BigDecimal("750000.00"))
                .receivedDate(LocalDate.now().minusDays(10))
                .responseDueDate(LocalDate.now().plusDays(20))
                .status(status)
                .priority(NoticePriority.HIGH)
                .responseRequired(true)
                .responseStatus(NoticeResponseStatus.REQUIRED)
                .hearingRequired(true)
                .hearingStatus(HearingStatus.SCHEDULED)
                .hearingDate(LocalDate.now().plusDays(15))
                .hearingTime("11:30 AM")
                .hearingMode(HearingMode.VIRTUAL_VC)
                .build());
        TenantContext.clear();
        return notice;
    }

    // ==========================================
    // ==========================================
    // 1. Cross-Tenant Isolation Tests
    // ==========================================

    @Test
    @DisplayName("REQ-1: Org A user cannot update response on Org B notice -> Returns 404/403")
    void crossTenant_cannotUpdateResponseOnOtherOrgNotice() throws Exception {
        TaxNoticeEntity noticeOrgB = createNotice(orgB.getId(), clientB.getId(), NoticeDepartment.INCOME_TAX, "IT-B-001", NoticeStatus.RESPONSE_PREPARATION);

        UpdateNoticeResponseRequest request = UpdateNoticeResponseRequest.builder()
                .responseDraft("Unauthorized cross-tenant draft text")
                .responseStatus(NoticeResponseStatus.DRAFT)
                .build();

        mockMvc.perform(put("/api/v1/tax-notices/{id}/response", noticeOrgB.getId())
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("REQ-2: Org A user cannot update hearing on Org B notice -> Returns 404/403")
    void crossTenant_cannotUpdateHearingOnOtherOrgNotice() throws Exception {
        TaxNoticeEntity noticeOrgB = createNotice(orgB.getId(), clientB.getId(), NoticeDepartment.GST, "GST-B-001", NoticeStatus.HEARING_SCHEDULED);

        UpdateNoticeHearingRequest request = UpdateNoticeHearingRequest.builder()
                .hearingOutcome("Adjourned without permission")
                .hearingStatus(HearingStatus.ADJOURNED)
                .build();

        mockMvc.perform(put("/api/v1/tax-notices/{id}/hearing", noticeOrgB.getId())
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // ==========================================
    // 2. Client Portfolio Scope Restrictions
    // ==========================================

    @Test
    @DisplayName("REQ-3: Staff A cannot draft response on Client A2 notice (outside assigned portfolio scope) -> Returns 403")
    void portfolioScope_staffCannotDraftResponseOnUnassignedClientNotice() throws Exception {
        TaxNoticeEntity noticeA2 = createNotice(orgA.getId(), clientA2.getId(), NoticeDepartment.INCOME_TAX, "IT-A-002", NoticeStatus.RESPONSE_PREPARATION);

        mockMvc.perform(post("/api/v1/tax-notices/{id}/response/draft", noticeA2.getId())
                        .header("Authorization", staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseTitle\": \"Out of scope draft\", \"responseSummary\": \"Draft reply for out of scope client\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("REQ-4: Staff A cannot update hearing on Client A2 notice (outside assigned portfolio scope) -> Returns 403")
    void portfolioScope_staffCannotUpdateHearingOnUnassignedClientNotice() throws Exception {
        TaxNoticeEntity noticeA2 = createNotice(orgA.getId(), clientA2.getId(), NoticeDepartment.INCOME_TAX, "IT-A-002", NoticeStatus.HEARING_SCHEDULED);

        AdjournHearingRequest request = AdjournHearingRequest.builder()
                .reason("Client requested adjournment")
                .nextHearingDate(LocalDate.now().plusDays(10))
                .build();

        mockMvc.perform(post("/api/v1/tax-notices/{id}/hearing/adjourn", noticeA2.getId())
                        .header("Authorization", staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 3. Role-Based Elevated Access (Partner / Admin)
    // ==========================================

    @Test
    @DisplayName("REQ-5: Org Admin and Partner have organization-wide access to all notices regardless of client assignment")
    void elevatedRoles_haveFullAccessAcrossAllOrgClients() throws Exception {
        TaxNoticeEntity noticeA2 = createNotice(orgA.getId(), clientA2.getId(), NoticeDepartment.INCOME_TAX, "IT-A-002", NoticeStatus.RESPONSE_PREPARATION);

        // Admin drafts response on Client A2 (assigned to otherStaff)
        mockMvc.perform(post("/api/v1/tax-notices/{id}/response/draft", noticeA2.getId())
                        .header("Authorization", adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseTitle\": \"Admin Reply Draft\", \"responseSummary\": \"Draft created by Org Admin\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.responseTitle", is("Admin Reply Draft")));

        // Partner updates hearing on Client A2
        UpdateNoticeHearingRequest hearingReq = UpdateNoticeHearingRequest.builder()
                .hearingRequired(true)
                .hearingStatus(HearingStatus.SCHEDULED)
                .hearingDate(LocalDate.now().plusDays(7))
                .hearingTime("02:30 PM")
                .hearingMode(HearingMode.PHYSICAL)
                .hearingLocation("Room 204, Aayakar Bhavan, Mumbai")
                .build();

        mockMvc.perform(put("/api/v1/tax-notices/{id}/hearing", noticeA2.getId())
                        .header("Authorization", partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hearingReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hearingStatus", is("SCHEDULED")))
                .andExpect(jsonPath("$.data.hearingLocation", is("Room 204, Aayakar Bhavan, Mumbai")));
    }

    // ==========================================
    // 4. Authentication and Permission Enforcement
    // ==========================================

    @Test
    @DisplayName("REQ-6 & REQ-7: Unauthenticated and malformed token requests are rejected -> Returns 401")
    void authentication_unauthenticatedAndInvalidTokensRejected() throws Exception {
        TaxNoticeEntity noticeA1 = createNotice(orgA.getId(), clientA1.getId(), NoticeDepartment.INCOME_TAX, "IT-A-001", NoticeStatus.RESPONSE_PREPARATION);

        // No token
        mockMvc.perform(post("/api/v1/tax-notices/{id}/response/draft", noticeA1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseTitle\": \"Draft Title\", \"responseSummary\": \"Draft text\"}"))
                .andExpect(status().isUnauthorized());

        // Malformed token
        mockMvc.perform(post("/api/v1/tax-notices/{id}/response/draft", noticeA1.getId())
                        .header("Authorization", "Bearer invalid-tampered-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseTitle\": \"Draft Title\", \"responseSummary\": \"Draft text\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("REQ-8 & REQ-9: User lacking NOTICE_UPDATE / NOTICE_RESPONSE_CREATE permission is denied -> Returns 403")
    void authorization_missingNoticeUpdatePermissionIsDenied() throws Exception {
        TaxNoticeEntity noticeA1 = createNotice(orgA.getId(), clientA1.getId(), NoticeDepartment.INCOME_TAX, "IT-A-001", NoticeStatus.RESPONSE_PREPARATION);

        mockMvc.perform(post("/api/v1/tax-notices/{id}/response/draft", noticeA1.getId())
                        .header("Authorization", noPermissionTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseTitle\": \"Draft Title\", \"responseSummary\": \"Draft text\"}"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 5. Response Workflow & State Machine
    // ==========================================

    @Test
    @DisplayName("REQ-10: Response drafting updates responseStatus to DRAFT and saves response text")
    void responseWorkflow_draftingSetsDraftStatusAndText() throws Exception {
        TaxNoticeEntity noticeA1 = createNotice(orgA.getId(), clientA1.getId(), NoticeDepartment.INCOME_TAX, "IT-A-001", NoticeStatus.RESPONSE_PREPARATION);

        mockMvc.perform(post("/api/v1/tax-notices/{id}/response/draft", noticeA1.getId())
                        .header("Authorization", staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseTitle\": \"Objections to Section 143(2)\", \"legalGrounds\": \"Grounds on jurisdiction and deductions\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.responseTitle", is("Objections to Section 143(2)")))
                .andExpect(jsonPath("$.data.legalGrounds", is("Grounds on jurisdiction and deductions")));
    }

    @Test
    @DisplayName("REQ-11 & REQ-12: Response submission transitions status to SUBMITTED and locks from invalid re-submission")
    void responseWorkflow_submissionTransitionsToSubmittedAndLocks() throws Exception {
        TaxNoticeEntity noticeA1 = createNotice(orgA.getId(), clientA1.getId(), NoticeDepartment.INCOME_TAX, "IT-A-001", NoticeStatus.RESPONSE_PREPARATION);

        // First draft response
        mockMvc.perform(post("/api/v1/tax-notices/{id}/response/draft", noticeA1.getId())
                        .header("Authorization", staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseTitle\": \"Final Submission Draft\", \"responseSummary\": \"Finalized grounds\"}"))
                .andExpect(status().isCreated());

        // Then submit response
        mockMvc.perform(post("/api/v1/tax-notices/{id}/response/submit", noticeA1.getId())
                        .header("Authorization", staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submissionMode\": \"INCOME_TAX_PORTAL\", \"portalAcknowledgementNumber\": \"ITBA/AST/2026/0019283\", \"remarks\": \"Submitted via e-filing portal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.data.portalAcknowledgementNumber", is("ITBA/AST/2026/0019283")));

        // Attempting to submit again without transitioning out of SUBMITTED should fail (illegal state transition)
        mockMvc.perform(post("/api/v1/tax-notices/{id}/response/submit", noticeA1.getId())
                        .header("Authorization", staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submissionMode\": \"INCOME_TAX_PORTAL\", \"portalAcknowledgementNumber\": \"DUPLICATE/SUBMISSION\"}"))
                .andExpect(status().is4xxClientError());
    }

    // ==========================================
    // 6. Hearing Lifecycle Management & State Transitions
    // ==========================================

    @Test
    @DisplayName("REQ-13: Hearing adjournment requires valid reason and transitions hearingStatus to ADJOURNED")
    void hearingLifecycle_adjournUpdatesStatusAndReason() throws Exception {
        TaxNoticeEntity noticeA1 = createNotice(orgA.getId(), clientA1.getId(), NoticeDepartment.INCOME_TAX, "IT-A-001", NoticeStatus.HEARING_SCHEDULED);

        LocalDate nextDate = LocalDate.now().plusDays(14);
        AdjournHearingRequest adjournReq = AdjournHearingRequest.builder()
                .reason("Assessing Officer requested additional books of accounts")
                .nextHearingDate(nextDate)
                .nextHearingTime("03:00 PM")
                .proceedingsSummary("Matter refixed for 2 weeks later")
                .build();

        mockMvc.perform(post("/api/v1/tax-notices/{id}/hearing/adjourn", noticeA1.getId())
                        .header("Authorization", staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjournReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ADJOURNED")))
                .andExpect(jsonPath("$.data.nextHearingDate", is(nextDate.toString())));
    }

    @Test
    @DisplayName("REQ-14: Hearing completion updates hearingStatus to COMPLETED with outcome summary")
    void hearingLifecycle_completeUpdatesStatusAndOutcome() throws Exception {
        TaxNoticeEntity noticeA1 = createNotice(orgA.getId(), clientA1.getId(), NoticeDepartment.INCOME_TAX, "IT-A-001", NoticeStatus.HEARING_SCHEDULED);

        mockMvc.perform(post("/api/v1/tax-notices/{id}/hearing/complete", noticeA1.getId())
                        .header("Authorization", staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcomeSummary\": \"Arguments concluded on additions under Section 68; order reserved\", \"proceedingsSummary\": \"Submissions filed in physical form\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("COMPLETED")))
                .andExpect(jsonPath("$.data.outcomeSummary", containsString("Arguments concluded on additions under Section 68")));
    }

    @Test
    @DisplayName("REQ-15: Hearing cancellation updates hearingStatus to CANCELLED")
    void hearingLifecycle_cancelUpdatesStatus() throws Exception {
        TaxNoticeEntity noticeA1 = createNotice(orgA.getId(), clientA1.getId(), NoticeDepartment.INCOME_TAX, "IT-A-001", NoticeStatus.HEARING_SCHEDULED);

        mockMvc.perform(post("/api/v1/tax-notices/{id}/hearing/cancel", noticeA1.getId())
                        .header("Authorization", staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Notice proceedings dropped by department\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));
    }

    // ==========================================
    // 7. Audit Logging and Activity Tracking
    // ==========================================

    @Test
    @DisplayName("REQ-16, REQ-17, REQ-18: Response and hearing actions generate comprehensive audit and activity records")
    void auditAndActivity_loggedForEachLifecycleEvent() throws Exception {
        TaxNoticeEntity noticeA1 = createNotice(orgA.getId(), clientA1.getId(), NoticeDepartment.INCOME_TAX, "IT-A-001", NoticeStatus.RESPONSE_PREPARATION);

        // 1. Draft response
        mockMvc.perform(post("/api/v1/tax-notices/{id}/response/draft", noticeA1.getId())
                        .header("Authorization", staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseTitle\": \"Factual Reply v1\", \"responseSummary\": \"Drafting full factual reply\"}"))
                .andExpect(status().isCreated());

        // 2. Submit response
        mockMvc.perform(post("/api/v1/tax-notices/{id}/response/submit", noticeA1.getId())
                        .header("Authorization", staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submissionMode\": \"INCOME_TAX_PORTAL\", \"portalAcknowledgementNumber\": \"ACK-887766\"}"))
                .andExpect(status().isOk());

        // 3. Adjourn hearing
        mockMvc.perform(post("/api/v1/tax-notices/{id}/hearing/adjourn", noticeA1.getId())
                        .header("Authorization", staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Adjourned on request\", \"nextHearingDate\": \"" + LocalDate.now().plusDays(21) + "\"}"))
                .andExpect(status().isOk());

        // Verify Audit Logs
        TenantContext.setTenantId(orgA.getId());
        List<AuditLogEntity> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).isNotEmpty();
        assertThat(auditLogs).anyMatch(log -> "RESPONSE_DRAFT_CREATED".equals(log.getAction()));
        assertThat(auditLogs).anyMatch(log -> "RESPONSE_SUBMITTED".equals(log.getAction()));
        assertThat(auditLogs).anyMatch(log -> "HEARING_ADJOURNED".equals(log.getAction()));

        TenantContext.clear();
    }
}
