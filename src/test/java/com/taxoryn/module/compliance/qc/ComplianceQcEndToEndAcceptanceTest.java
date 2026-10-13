package com.taxoryn.module.compliance.qc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.dto.ApproveWorkflowRequest;
import com.taxoryn.module.compliance.dto.AssignComplianceWorkflowRequest;
import com.taxoryn.module.compliance.dto.CompleteComplianceWorkflowRequest;
import com.taxoryn.module.compliance.dto.MarkWorkflowFiledRequest;
import com.taxoryn.module.compliance.dto.RequestWorkflowChangesRequest;
import com.taxoryn.module.compliance.dto.UpdateChecklistItemRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceWorkflowStatusRequest;
import com.taxoryn.module.compliance.dto.WaitClientWorkflowRequest;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.qc.dto.AuthorizeFilingRequest;
import com.taxoryn.module.compliance.qc.dto.CreateReviewFindingRequest;
import com.taxoryn.module.compliance.qc.dto.ResolveFindingRequest;
import com.taxoryn.module.compliance.qc.dto.RespondToFindingRequest;
import com.taxoryn.module.compliance.qc.dto.RevokeAuthorizationRequest;
import com.taxoryn.module.compliance.qc.model.ComplianceAuthorizationMethod;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowChecklistItemRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.PermissionRepository;
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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 31.5.1 End-to-End Compliance Quality Control Acceptance Test Suite.
 * Validates cross-slice orchestration across Phase 31.1 through 31.4:
 * Scenario A: Successful Full Lifecycle (Preparation to Completion)
 * Scenario B: Review Rejection and Correction Cycle (Findings, Responses, Stale Validation)
 * Scenario C: Authoritative Approval Gates (Status, Reviewer Entitlement, Four-Eyes)
 * Scenario D: Authorization, Revocation & Re-authorization Provenance
 * Scenario E: Submission and Completion Bypass Closures
 * Scenario F: Multi-Tenant and Practice Scope Isolation
 * Scenario G: Regression, Compatibility & Audit Trail Stability
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplianceQcEndToEndAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComplianceWorkflowRepository workflowRepository;

    @Autowired
    private ComplianceWorkflowChecklistItemRepository checklistItemRepository;

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
    private EmployeeRepository employeeRepository;

    @Autowired
    private com.taxoryn.module.compliance.qc.repository.ComplianceQcReviewFindingRepository findingRepository;

    @Autowired
    private com.taxoryn.module.compliance.qc.repository.ComplianceQcAuthorizationRecordRepository authorizationRecordRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org;
    private LocationEntity location;
    private UserEntity adminUser;
    private String adminToken;
    private UserEntity makerUser;
    private EmployeeEntity makerEmployee;
    private String makerToken;
    private UserEntity reviewerUser;
    private EmployeeEntity reviewerEmployee;
    private String reviewerToken;
    private UserEntity bystanderUser;
    private EmployeeEntity bystanderEmployee;
    private String bystanderToken;
    private ClientEntity client;
    private ComplianceObligationEntity obligation;

    // Cross-tenant fixtures
    private OrganizationEntity otherOrg;
    private UserEntity otherUser;
    private String otherToken;

    @BeforeEach
    void setUp() {
        cleanUp();

        org = organizationRepository.save(OrganizationEntity.builder()
                .name("Kothari & Associates LLP")
                .legalName("Kothari & Associates LLP")
                .status(OrganizationStatus.ACTIVE)
                .email("contact-" + UUID.randomUUID() + "@kothari.in")
                .build());

        LocationEntity loc = LocationEntity.builder()
                .name("Bengaluru Office")
                .code("BLR-01")
                .city("Bengaluru")
                .state("Karnataka")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        loc.setOrganizationId(org.getId());
        location = locationRepository.save(loc);

        // Core permissions
        PermissionEntity submitPerm = permissionRepository.findByCode("REVIEW_SUBMIT")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder()
                        .code("REVIEW_SUBMIT").name("Submit Review").module("REVIEW").build()));

        PermissionEntity approvePerm = permissionRepository.findByCode("REVIEW_APPROVE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder()
                        .code("REVIEW_APPROVE").name("Approve Review").module("REVIEW").build()));

        PermissionEntity rejectPerm = permissionRepository.findByCode("REVIEW_REJECT")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder()
                        .code("REVIEW_REJECT").name("Reject Review").module("REVIEW").build()));

        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity reviewerRole = roleRepository.save(RoleEntity.builder()
                .code("MANAGER")
                .name("Tax Manager Reviewer")
                .isSystemRole(true)
                .permissions(new HashSet<>(Set.of(approvePerm, rejectPerm)))
                .build());

        RoleEntity makerRole = roleRepository.save(RoleEntity.builder()
                .code("STAFF")
                .name("Maker Preparer")
                .isSystemRole(true)
                .permissions(new HashSet<>(Set.of(submitPerm)))
                .build());

        RoleEntity bystanderRole = roleRepository.save(RoleEntity.builder()
                .code("AUDITOR")
                .name("Internal Auditor")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // Admin User
        adminUser = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("admin-" + UUID.randomUUID() + "@kothari.com")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .firstName("Suresh")
                .lastName("Kothari")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                org.getId(),
                adminUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_EDIT", "TASK_VIEW", "TASK_CREATE", "TASK_EDIT", "TASK_ASSIGN", "GST_VIEW", "GST_EDIT", "REVIEW_APPROVE", "COMPLIANCE_EDIT")
        );

        // Maker User & Employee
        makerUser = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("preparer-" + UUID.randomUUID() + "@kothari.com")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .firstName("Deepak")
                .lastName("Verma")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(makerRole))
                .build());

        EmployeeEntity makerEmp = EmployeeEntity.builder()
                .employeeCode("EMP-PRP-" + UUID.randomUUID().toString().substring(0, 6))
                .firstName("Deepak")
                .lastName("Verma")
                .email(makerUser.getEmail())
                .department("Tax Compliance")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .userId(makerUser.getId())
                .build();
        makerEmp.setOrganizationId(org.getId());
        makerEmployee = employeeRepository.save(makerEmp);

        makerToken = jwtTokenProvider.generateAccessToken(
                makerUser.getId(),
                org.getId(),
                makerUser.getEmail(),
                Set.of("ROLE_STAFF"),
                Set.of("ROLE_STAFF", "CLIENT_VIEW", "CLIENT_EDIT", "TASK_VIEW", "TASK_CREATE", "TASK_EDIT", "GST_VIEW", "GST_EDIT", "REVIEW_SUBMIT")
        );

        // Reviewer User & Employee
        reviewerUser = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("reviewer-" + UUID.randomUUID() + "@kothari.com")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .firstName("Ananya")
                .lastName("Deshmukh")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(reviewerRole))
                .build());

        EmployeeEntity revEmp = EmployeeEntity.builder()
                .employeeCode("EMP-REV-" + UUID.randomUUID().toString().substring(0, 6))
                .firstName("Ananya")
                .lastName("Deshmukh")
                .email(reviewerUser.getEmail())
                .department("Tax Compliance")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .userId(reviewerUser.getId())
                .build();
        revEmp.setOrganizationId(org.getId());
        reviewerEmployee = employeeRepository.save(revEmp);

        reviewerToken = jwtTokenProvider.generateAccessToken(
                reviewerUser.getId(),
                org.getId(),
                reviewerUser.getEmail(),
                Set.of("ROLE_MANAGER"),
                Set.of("ROLE_MANAGER", "CLIENT_VIEW", "CLIENT_EDIT", "TASK_VIEW", "TASK_EDIT", "GST_VIEW", "GST_EDIT", "REVIEW_APPROVE", "REVIEW_REJECT", "REVIEW_VIEW")
        );

        // Bystander User (arbitrary employee without review permissions)
        bystanderUser = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("bystander-" + UUID.randomUUID() + "@kothari.com")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .firstName("Rohan")
                .lastName("Gupta")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(bystanderRole))
                .build());

        EmployeeEntity byEmp = EmployeeEntity.builder()
                .employeeCode("EMP-BYST-" + UUID.randomUUID().toString().substring(0, 6))
                .firstName("Rohan")
                .lastName("Gupta")
                .email(bystanderUser.getEmail())
                .department("Audit")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .userId(bystanderUser.getId())
                .build();
        byEmp.setOrganizationId(org.getId());
        bystanderEmployee = employeeRepository.save(byEmp);

        bystanderToken = jwtTokenProvider.generateAccessToken(
                bystanderUser.getId(),
                org.getId(),
                bystanderUser.getEmail(),
                Set.of("ROLE_AUDITOR"),
                Set.of("ROLE_AUDITOR", "CLIENT_VIEW", "TASK_VIEW")
        );

        // Client & Obligation
        ClientEntity clientEntity = ClientEntity.builder()
                .displayName("Zycon Technologies")
                .legalName("Zycon Technologies Private Limited")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .pan("ZYCON1234K")
                .gstin("29ZYCON1234K1Z8")
                .locationId(location.getId())
                .assignedEmployeeId(makerEmployee.getId())
                .build();
        clientEntity.setOrganizationId(org.getId());
        client = clientRepository.save(clientEntity);

        ComplianceObligationEntity ob = ComplianceObligationEntity.builder()
                .clientId(client.getId())
                .locationId(location.getId())
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("GSTR-3B Return Filing for August 2026")
                .statutoryDueDate(LocalDate.now().plusDays(10))
                .internalTargetDate(LocalDate.now().plusDays(5))
                .status(ComplianceObligationStatus.READY)
                .assignedUserId(makerUser.getId())
                .assignedEmployeeId(makerEmployee.getId())
                .build();
        ob.setOrganizationId(org.getId());
        obligation = obligationRepository.save(ob);

        // Cross-Tenant Fixtures
        otherOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Cross-Tenant Competitor LLP")
                .legalName("Cross-Tenant Competitor LLP")
                .status(OrganizationStatus.ACTIVE)
                .email("other-" + UUID.randomUUID() + "@competitor.in")
                .build());

        otherUser = userRepository.save(UserEntity.builder()
                .organizationId(otherOrg.getId())
                .email("spy-" + UUID.randomUUID() + "@competitor.com")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .firstName("Malicious")
                .lastName("Actor")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        otherToken = jwtTokenProvider.generateAccessToken(
                otherUser.getId(),
                otherOrg.getId(),
                otherUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_EDIT", "TASK_VIEW", "TASK_EDIT", "REVIEW_APPROVE", "COMPLIANCE_EDIT")
        );
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        com.taxoryn.core.security.TenantContext.clear();
    }

    private void cleanUp() {
        authorizationRecordRepository.deleteAll();
        findingRepository.deleteAll();
        checklistItemRepository.deleteAll();
        workflowRepository.deleteAll();
        obligationRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    // =========================================================================
    // SCENARIO A: Complete Happy-Path End-to-End Acceptance
    // =========================================================================

    @Test
    @DisplayName("Scenario A: Full lifecycle from Preparation to Validation, Review, Approval, Authorization, Filing and Completion")
    void testScenarioA_CompleteEndToEndLifecycle_PreparationToCompletion() throws Exception {
        // 1. Create Workflow
        String createResp = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID workflowId = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

        // 2. Assign Maker and Reviewer
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AssignComplianceWorkflowRequest.builder()
                                .assignedEmployeeId(makerEmployee.getId())
                                .reviewerEmployeeId(reviewerEmployee.getId())
                                .build())))
                .andExpect(status().isOk());

        // 3. Start Preparation
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/start")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("IN_PROGRESS"));

        // 4. Pre-review Validation before checklist completion -> Validates rule engine
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));

        // 5. Submit for Review
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("UNDER_REVIEW"));

        // 6. Preparer (maker) cannot approve own work (Four-Eyes Principle)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().approvalNotes("Self approve").build())))
                .andExpect(status().isForbidden());

        // 7. Assigned Reviewer approves
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().approvalNotes("Return computation verified and approved").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("READY_FOR_FILING"));

        // 8. Attempt submission before authorization -> Blocked
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/mark-filed")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MarkWorkflowFiledRequest.builder()
                                .filedDate(LocalDate.now())
                                .acknowledgementNumber("ARN-PREMATURE")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("filing authorization has not been granted")));

        // 9. Grant Filing Authorization with AUTHORIZED_SIGNATORY
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/authorize-filing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AuthorizeFilingRequest.builder()
                                .authorizationMethod(ComplianceAuthorizationMethod.AUTHORIZED_SIGNATORY)
                                .authorizedBy("Vikram Kothari (Managing Director)")
                                .authorizationReference("BOARD-RES-2026-AUG-11")
                                .authorizationNotes("Board authorization executed for statutory GST return")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorized").value(true))
                .andExpect(jsonPath("$.data.authorizationMethod").value("AUTHORIZED_SIGNATORY"));

        // Verify history contains GRANTED record
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/authorizations")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].action").value("GRANTED"))
                .andExpect(jsonPath("$.data[0].authorizedBy").value("Vikram Kothari (Managing Director)"));

        // 10. Record Portal Filing
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/mark-filed")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MarkWorkflowFiledRequest.builder()
                                .filedDate(LocalDate.now())
                                .acknowledgementNumber("ARN-KA2908260192837")
                                .filingNotes("Portal ARN generated successfully")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("FILED"))
                .andExpect(jsonPath("$.data.acknowledgementNumber").value("ARN-KA2908260192837"));

        // 11. Finalize and Complete Workflow
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/complete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CompleteComplianceWorkflowRequest.builder()
                                .acknowledgementNumber("ARN-KA2908260192837")
                                .notes("Acknowledgement receipt archived in DMS and copy sent to client.")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("COMPLETED"));

        // 12. Final QC Status check
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/status")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("SUBMISSION"))
                .andExpect(jsonPath("$.data.submitted").value(true));
    }

    // =========================================================================
    // SCENARIO B: Review Rejection and Correction Cycle
    // =========================================================================

    @Test
    @DisplayName("Scenario B: Review Rejection, Finding Responses, Reviewer Resolution, and Stale Fingerprint Protection")
    void testScenarioB_ReviewCycleFindingsCorrectionAndStaleFingerprintRejection() throws Exception {
        // 1. Setup workflow in UNDER_REVIEW
        String createResp = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID workflowId = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AssignComplianceWorkflowRequest.builder()
                                .assignedEmployeeId(makerEmployee.getId())
                                .reviewerEmployeeId(reviewerEmployee.getId())
                                .build())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/start")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        // 2. Reviewer records BLOCKING finding
        String findingResp = mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/findings")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateReviewFindingRequest.builder()
                                .category("COMPUTATION")
                                .severity("BLOCKING")
                                .description("Table 4(A)(5) all other ITC does not match GSTR-2B purchase register")
                                .correctiveGuidance("Reconcile ITC with 2B download and amend eligible credit")
                                .sourceReference("GSTR-2B-AUG-2026")
                                .build())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID findingId = UUID.fromString(objectMapper.readTree(findingResp).path("data").path("id").asText());

        // 3. Reviewer requests changes
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/request-changes")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RequestWorkflowChangesRequest.builder()
                                .reason("ITC discrepancy must be resolved before sign-off")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("CHANGES_REQUIRED"));

        // 4. Maker attempts to resolve finding directly -> Forbidden (reviewer-only)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/findings/" + findingId + "/resolve")
                        .header("Authorization", "Bearer " + makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ResolveFindingRequest.builder().decision("RESOLVED").build())))
                .andExpect(status().isForbidden());

        // 5. Maker responds to finding
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/findings/" + findingId + "/respond")
                        .header("Authorization", "Bearer " + makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RespondToFindingRequest.builder()
                                .response("Ineligible ITC of INR 45,000 reversed as per Section 17(5); Table 4 now reconciles with 2B.")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CORRECTED"));

        // 6. Maker toggles checklist checkpoint to complete preparation step
        var workflowEntity = workflowRepository.findByIdWithChecklist(workflowId, org.getId()).orElseThrow();
        UUID checkItemId = workflowEntity.getChecklistItems().get(0).getId();
        mockMvc.perform(patch("/api/v1/compliance/workflows/" + workflowId + "/checklist/" + checkItemId)
                        .header("Authorization", "Bearer " + makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateChecklistItemRequest.builder()
                                .isCompleted(true)
                                .notes("Reconciliation verified")
                                .build())))
                .andExpect(status().isOk());

        // 7. Resubmit review WITHOUT re-validation -> Blocked (stale fingerprint)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Validation result is stale")));

        // 8. Re-validate preparation -> Refreshes fingerprint
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));

        // 9. Resubmit review -> Succeeds, moves to Cycle 2
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.data.reviewCycle").value(2));

        // 10. Reviewer attempts to approve while finding is still CORRECTED (not yet resolved) -> Fails
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().approvalNotes("Premature").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("blocking review finding(s) remain unresolved")));

        // 11. Reviewer resolves finding
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/findings/" + findingId + "/resolve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ResolveFindingRequest.builder()
                                .decision("RESOLVED")
                                .reviewerNotes("Verified Table 4 ITC reversal against 2B; satisfactory.")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        // 12. Reviewer approves workflow -> Succeeds
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().approvalNotes("Cycle 2 correction verified. Approved.").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("READY_FOR_FILING"));
    }

    // =========================================================================
    // SCENARIO C: Approval Gates Edge Cases
    // =========================================================================

    @Test
    @DisplayName("Scenario C: Approval Gates Reject Non-UNDER_REVIEW, Unauthorized or Unassigned Actors")
    void testScenarioC_ApprovalGates_EnforcesReviewerIdentityAndAuthority() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID workflowId = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AssignComplianceWorkflowRequest.builder()
                                .assignedEmployeeId(makerEmployee.getId())
                                .reviewerEmployeeId(reviewerEmployee.getId())
                                .build())))
                .andExpect(status().isOk());

        // 1. Attempt to approve when NOT_STARTED / CREATED -> Fails (400)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().approvalNotes("Premature").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("UNDER_REVIEW")));

        // Start workflow -> IN_PROGRESS
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/start")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        // 2. Attempt to approve when IN_PROGRESS -> Fails (400)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().approvalNotes("Premature").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("UNDER_REVIEW")));

        // Transition to WAITING_FOR_CLIENT
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/wait-client")
                        .header("Authorization", "Bearer " + makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(WaitClientWorkflowRequest.builder()
                                .reason("Awaiting bank statements")
                                .build())))
                .andExpect(status().isOk());

        // 3. Attempt to approve when WAITING_FOR_CLIENT -> Fails (400)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().approvalNotes("Premature").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("UNDER_REVIEW")));

        // Resume to IN_PROGRESS
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/resume-client")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        // Validate and submit for review -> UNDER_REVIEW
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        // 4. Bystander (without REVIEW_APPROVE) attempts approval -> Forbidden (403)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + bystanderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().approvalNotes("Bystander").build())))
                .andExpect(status().isForbidden());

        // 5. Assigned reviewer approves successfully -> 200
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().approvalNotes("Authorized approval").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("READY_FOR_FILING"));
    }

    // =========================================================================
    // SCENARIO D: Authorization, Revocation & Re-authorization Provenance
    // =========================================================================

    @Test
    @DisplayName("Scenario D: Authorization, Revocation with Reason, Reauthorization and Full Audit Trail Stability")
    void testScenarioD_AuthorizationRevocationAndReauthorizationHistory() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID workflowId = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AssignComplianceWorkflowRequest.builder()
                                .assignedEmployeeId(makerEmployee.getId())
                                .reviewerEmployeeId(reviewerEmployee.getId())
                                .build())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/start")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        // 1. Attempt authorization before approval (while IN_PROGRESS) -> Fails (400)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/authorize-filing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AuthorizeFilingRequest.builder()
                                .authorizationMethod(ComplianceAuthorizationMethod.CLIENT_CONSENT)
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("READY_FOR_FILING")));

        // Progress to READY_FOR_FILING
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().approvalNotes("Approved").build())))
                .andExpect(status().isOk());

        // 2. First Authorization Grant (DSC_SIGN_OFF) -> Record 1 (GRANTED)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/authorize-filing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AuthorizeFilingRequest.builder()
                                .authorizationMethod(ComplianceAuthorizationMethod.DSC_SIGN_OFF)
                                .authorizedBy("Rajesh Gupta (CFO)")
                                .authorizationReference("DSC-TOKEN-2026-08-14")
                                .authorizationNotes("Signed via Class 3 DSC certificate")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorized").value(true));

        // 3. Duplicate Grant Attempt -> Fails (400, already authorized)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/authorize-filing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AuthorizeFilingRequest.builder()
                                .authorizationMethod(ComplianceAuthorizationMethod.DSC_SIGN_OFF)
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already authorized")));

        // 4. Revocation without reason -> Fails (400, bean validation)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/revoke-authorization")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RevokeAuthorizationRequest.builder().reason("").build())))
                .andExpect(status().isBadRequest());

        // 5. Valid Revocation -> Record 2 (REVOKED)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/revoke-authorization")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RevokeAuthorizationRequest.builder()
                                .reason("Client reported discrepancy in sales credit notes requiring revision")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorized").value(false));

        // 6. Duplicate Revocation Attempt -> Fails (400, not authorized)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/revoke-authorization")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RevokeAuthorizationRequest.builder()
                                .reason("Repeated call")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not currently authorized")));

        // 7. Attempt to file while revoked -> Blocked by QC gate
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/mark-filed")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MarkWorkflowFiledRequest.builder()
                                .filedDate(LocalDate.now())
                                .acknowledgementNumber("ARN-REVOKED-TEST")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("explicit filing authorization has not been granted")));

        // 8. Re-authorization Grant -> Record 3 (GRANTED)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/authorize-filing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AuthorizeFilingRequest.builder()
                                .authorizationMethod(ComplianceAuthorizationMethod.PORTAL_CREDENTIALS)
                                .authorizedBy("Rajesh Gupta (CFO)")
                                .authorizationReference("PORTAL-CONFIRM-99218")
                                .authorizationNotes("Credit notes verified and final consent recorded on portal")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorized").value(true));

        // 9. Verify History contains all 3 records ordered newest first
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/authorizations")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].action").value("GRANTED"))
                .andExpect(jsonPath("$.data[0].authorizationMethod").value("PORTAL_CREDENTIALS"))
                .andExpect(jsonPath("$.data[1].action").value("REVOKED"))
                .andExpect(jsonPath("$.data[1].revocationReason").value("Client reported discrepancy in sales credit notes requiring revision"))
                .andExpect(jsonPath("$.data[2].action").value("GRANTED"))
                .andExpect(jsonPath("$.data[2].authorizationMethod").value("DSC_SIGN_OFF"));

        // 10. Filing now succeeds
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/mark-filed")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MarkWorkflowFiledRequest.builder()
                                .filedDate(LocalDate.now())
                                .acknowledgementNumber("ARN-REAUTH-SUCCESS")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("FILED"));
    }

    // =========================================================================
    // SCENARIO E: Submission and Completion Bypass Closures
    // =========================================================================

    @Test
    @DisplayName("Scenario E: Sealed Submission Bypasses across PUT /status and POST /complete")
    void testScenarioE_SubmissionAndCompletionBypassesSealed() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID workflowId = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AssignComplianceWorkflowRequest.builder()
                                .assignedEmployeeId(makerEmployee.getId())
                                .reviewerEmployeeId(reviewerEmployee.getId())
                                .build())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/start")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        // 1. Direct status jumps via PUT /status from IN_PROGRESS
        mockMvc.perform(put("/api/v1/compliance/workflows/" + workflowId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateComplianceWorkflowStatusRequest.builder()
                                .status(ComplianceWorkflowStatus.READY_FOR_FILING)
                                .build())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/compliance/workflows/" + workflowId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateComplianceWorkflowStatusRequest.builder()
                                .status(ComplianceWorkflowStatus.FILED)
                                .build())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/compliance/workflows/" + workflowId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateComplianceWorkflowStatusRequest.builder()
                                .status(ComplianceWorkflowStatus.COMPLETED)
                                .build())))
                .andExpect(status().isBadRequest());

        // 2. Direct POST /complete on IN_PROGRESS -> Blocked
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/complete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CompleteComplianceWorkflowRequest.builder()
                                .notes("Premature complete")
                                .build())))
                .andExpect(status().isBadRequest());

        // 3. Move to UNDER_REVIEW
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        // Direct PUT /status to FILED while UNDER_REVIEW -> Blocked
        mockMvc.perform(put("/api/v1/compliance/workflows/" + workflowId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateComplianceWorkflowStatusRequest.builder()
                                .status(ComplianceWorkflowStatus.FILED)
                                .build())))
                .andExpect(status().isBadRequest());

        // Direct POST /complete while UNDER_REVIEW -> Blocked
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/complete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CompleteComplianceWorkflowRequest.builder()
                                .notes("Premature complete")
                                .build())))
                .andExpect(status().isBadRequest());

        // 4. Approve workflow -> READY_FOR_FILING (unauthorized)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().approvalNotes("Approved").build())))
                .andExpect(status().isOk());

        // Direct POST /complete on READY_FOR_FILING without authorization -> Blocked
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/complete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CompleteComplianceWorkflowRequest.builder()
                                .notes("Premature complete")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("filing authorization has not been granted")));

        // Authorize and Mark Filed
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/authorize-filing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AuthorizeFilingRequest.builder()
                                .authorizationMethod(ComplianceAuthorizationMethod.CLIENT_CONSENT)
                                .build())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/mark-filed")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MarkWorkflowFiledRequest.builder()
                                .filedDate(LocalDate.now())
                                .acknowledgementNumber("ARN-IDEMPOTENCY-TEST")
                                .build())))
                .andExpect(status().isOk());

        // 5. Legitimate Idempotency: Complete workflow once already FILED succeeds cleanly
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/complete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CompleteComplianceWorkflowRequest.builder()
                                .acknowledgementNumber("ARN-IDEMPOTENCY-TEST")
                                .notes("Archived successfully")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("COMPLETED"));
    }

    // =========================================================================
    // SCENARIO F: Multi-Tenant and Practice Scope Isolation
    // =========================================================================

    @Test
    @DisplayName("Scenario F: Strict Multi-Tenant and Practice Scope Isolation across QC Endpoints")
    void testScenarioF_TenantAndPracticeIsolation() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID workflowId = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

        // 1. Cross-Tenant GET workflow detail -> 404 Not Found
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        // 2. Cross-Tenant GET QC status -> 404 Not Found
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/status")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        // 3. Cross-Tenant GET authorizations history -> 404 Not Found
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/authorizations")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        // 4. Cross-Tenant POST authorize-filing -> 404 Not Found
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/authorize-filing")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AuthorizeFilingRequest.builder()
                                .authorizationMethod(ComplianceAuthorizationMethod.CLIENT_CONSENT)
                                .build())))
                .andExpect(status().isNotFound());

        // 5. Cross-Tenant POST revoke-authorization -> 404 Not Found
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/revoke-authorization")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RevokeAuthorizationRequest.builder()
                                .reason("Cross-tenant attempt")
                                .build())))
                .andExpect(status().isNotFound());

        // 6. Cross-Tenant POST approve -> 404 Not Found
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().approvalNotes("Intruder").build())))
                .andExpect(status().isNotFound());

        // 7. Cross-Tenant POST mark-filed -> 404 Not Found
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/mark-filed")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MarkWorkflowFiledRequest.builder()
                                .filedDate(LocalDate.now())
                                .acknowledgementNumber("ARN-CROSS-TENANT")
                                .build())))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // SCENARIO G: Regression, Compatibility & Audit Trail Stability
    // =========================================================================

    @Test
    @DisplayName("Scenario G: Schema V143 Compatibility and Audit Trail Integrity")
    void testScenarioG_RegressionAndCompatibility() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID workflowId = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AssignComplianceWorkflowRequest.builder()
                                .assignedEmployeeId(makerEmployee.getId())
                                .reviewerEmployeeId(reviewerEmployee.getId())
                                .build())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/start")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().approvalNotes("Approved").build())))
                .andExpect(status().isOk());

        // Grant authorization
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/authorize-filing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AuthorizeFilingRequest.builder()
                                .authorizationMethod(ComplianceAuthorizationMethod.PRACTITIONER_AUTHORIZATION)
                                .authorizedBy("CA Suresh Kothari (Partner)")
                                .authorizationReference("PARTNER-DECISION-2026-08")
                                .authorizationNotes("Authorized under practice discretionary power for statutory return deadline")
                                .build())))
                .andExpect(status().isOk());

        // Query database repository directly to verify entity mappings and V143 columns
        var records = authorizationRecordRepository.findByOrganizationIdAndWorkflowIdOrderByCreatedAtDesc(org.getId(), workflowId);
        assertThat(records).hasSize(1);
        var record = records.get(0);
        assertThat(record.getAction()).isEqualTo("GRANTED");
        assertThat(record.getAuthorizationMethod()).isEqualTo("PRACTITIONER_AUTHORIZATION");
        assertThat(record.getAuthorizedBy()).isEqualTo("CA Suresh Kothari (Partner)");
        assertThat(record.getAuthorizationReference()).isEqualTo("PARTNER-DECISION-2026-08");
        assertThat(record.getActorEmail()).isEqualTo(adminUser.getEmail());
        assertThat(record.getActorUserId()).isEqualTo(adminUser.getId());
        assertThat(record.getCreatedAt()).isNotNull();
        assertThat(record.getOrganizationId()).isEqualTo(org.getId());
    }
}
