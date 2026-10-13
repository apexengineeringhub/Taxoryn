package com.taxoryn.module.compliance.qc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.dto.ApproveWorkflowRequest;
import com.taxoryn.module.compliance.dto.AssignComplianceWorkflowRequest;
import com.taxoryn.module.compliance.dto.MarkWorkflowFiledRequest;
import com.taxoryn.module.compliance.dto.RequestWorkflowChangesRequest;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.qc.dto.AuthorizeFilingRequest;
import com.taxoryn.module.compliance.qc.dto.RevokeAuthorizationRequest;
import com.taxoryn.module.compliance.qc.model.ComplianceAuthorizationMethod;
import com.taxoryn.module.compliance.qc.model.ComplianceQcStage;
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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplianceQualityControlIntegrationTest {

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
                .name("Apex Advisory LLP")
                .legalName("Apex Advisory LLP")
                .status(OrganizationStatus.ACTIVE)
                .email("contact-" + UUID.randomUUID() + "@apexadvisory.in")
                .build());

        LocationEntity loc = LocationEntity.builder()
                .name("Mumbai Corporate Office")
                .code("MUM-01")
                .city("Mumbai")
                .state("Maharashtra")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        loc.setOrganizationId(org.getId());
        location = locationRepository.save(loc);

        // Permissions
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

        adminUser = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("admin-" + UUID.randomUUID() + "@apexadvisory.com")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .firstName("Apex")
                .lastName("Admin")
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

        makerUser = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("preparer-" + UUID.randomUUID() + "@apexadvisory.com")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .firstName("Arjun")
                .lastName("Nair")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(makerRole))
                .build());

        EmployeeEntity makerEmp = EmployeeEntity.builder()
                .employeeCode("EMP-PRP-" + UUID.randomUUID().toString().substring(0, 6))
                .firstName("Arjun")
                .lastName("Nair")
                .email(makerUser.getEmail())
                .department("Tax & Compliance")
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

        reviewerUser = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("reviewer-" + UUID.randomUUID() + "@apexadvisory.com")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .firstName("Meera")
                .lastName("Iyer")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(reviewerRole))
                .build());

        EmployeeEntity revEmp = EmployeeEntity.builder()
                .employeeCode("EMP-REV-" + UUID.randomUUID().toString().substring(0, 6))
                .firstName("Meera")
                .lastName("Iyer")
                .email(reviewerUser.getEmail())
                .department("Tax & Compliance")
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

        ClientEntity clientEnt = ClientEntity.builder()
                .displayName("Nexus Enterprises Pvt Ltd")
                .legalName("Nexus Enterprises Private Limited")
                .pan("AABCN1234F")
                .gstin("27AABCN1234F1Z5")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .locationId(location.getId())
                .assignedEmployeeId(makerEmployee.getId())
                .build();
        clientEnt.setOrganizationId(org.getId());
        client = clientRepository.save(clientEnt);

        ComplianceObligationEntity obEnt = ComplianceObligationEntity.builder()
                .clientId(client.getId())
                .locationId(location.getId())
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("GSTR-3B Monthly Return for September 2026")
                .financialYear("2026-27")
                .period("2026-09")
                .periodLabel("September 2026")
                .statutoryDueDate(LocalDate.of(2026, 10, 20))
                .internalTargetDate(LocalDate.of(2026, 10, 16))
                .status(ComplianceObligationStatus.IN_PROGRESS)
                .build();
        obEnt.setOrganizationId(org.getId());
        obligation = obligationRepository.save(obEnt);

        // Cross-tenant setup
        otherOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Unrelated Practice Corp")
                .legalName("Unrelated Practice Corp")
                .status(OrganizationStatus.ACTIVE)
                .email("other-" + UUID.randomUUID() + "@unrelated.in")
                .build());

        otherUser = userRepository.save(UserEntity.builder()
                .organizationId(otherOrg.getId())
                .email("intruder-" + UUID.randomUUID() + "@unrelated.com")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .firstName("Eve")
                .lastName("Hacker")
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

    @org.junit.jupiter.api.AfterEach
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

    @Test
    @DisplayName("QC Lifecycle: Complete Controlled Progression: PREPARATION -> VALIDATION -> REVIEW -> CORRECTION -> RE-REVIEW -> APPROVAL -> AUTHORIZATION -> SUBMISSION")
    void testCompleteQualityControlProgression() throws Exception {
        // 1. Initialize workflow via obligation
        String createWorkflowResp = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID workflowId = UUID.fromString(objectMapper.readTree(createWorkflowResp).path("data").path("id").asText());

        // Assign maker and reviewer
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AssignComplianceWorkflowRequest.builder()
                                .assignedEmployeeId(makerEmployee.getId())
                                .reviewerEmployeeId(reviewerEmployee.getId())
                                .build())))
                .andExpect(status().isOk());

        // Start workflow -> IN_PROGRESS (PREPARATION stage)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/start")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        // Check QC Status at PREPARATION stage
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/status")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("PREPARATION"))
                .andExpect(jsonPath("$.data.workflowStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.approved").value(false))
                .andExpect(jsonPath("$.data.authorized").value(false))
                .andExpect(jsonPath("$.data.submitted").value(false));

        // 2. Run deterministic pre-review validation
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.blockers").isEmpty());

        // 3. Submit for Review -> REVIEW stage (UNDER_REVIEW)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("UNDER_REVIEW"));

        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/status")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("REVIEW"))
                .andExpect(jsonPath("$.data.underReview").value(true));

        // 4. Reviewer Requests Changes -> CORRECTION stage (CHANGES_REQUIRED)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/request-changes")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RequestWorkflowChangesRequest.builder()
                                .reason("Inward RCM supplies liability discrepancy. Recalculate Table 3.1(d).")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("CHANGES_REQUIRED"));

        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/status")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("CORRECTION"))
                .andExpect(jsonPath("$.data.changesRequired").value(true))
                .andExpect(jsonPath("$.data.nextActionableStep").value(containsString("Inward RCM supplies liability discrepancy")));

        // 5. Maker addresses findings, re-validates, and resubmits for review
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("UNDER_REVIEW"));

        // 6. Reviewer approves -> APPROVAL stage (READY_FOR_FILING, but NOT authorized)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder()
                                .approvalNotes("Table 3.1(d) RCM liability reconciled. Approved for filing authorization.")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("READY_FOR_FILING"));

        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/status")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("APPROVAL"))
                .andExpect(jsonPath("$.data.approved").value(true))
                .andExpect(jsonPath("$.data.authorized").value(false))
                .andExpect(jsonPath("$.data.nextActionableStep").value("Computation approved; obtain client / signatory filing authorization"));

        // 7. Verify submission prerequisite gate: CANNOT file without authorization!
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/mark-filed")
                        .header("Authorization", "Bearer " + makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MarkWorkflowFiledRequest.builder()
                                .filedDate(LocalDate.now())
                                .acknowledgementNumber("ARN-TEST-BLOCKED")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("filing authorization has not been granted")));

        // 8. Grant Filing Authorization -> AUTHORIZATION stage
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/authorize-filing")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AuthorizeFilingRequest.builder()
                                .authorizationMethod(ComplianceAuthorizationMethod.CLIENT_CONSENT)
                                .authorizationReference("EMAIL-SIGN-OFF-2026-10-18")
                                .authorizationNotes("Managing Director confirmed final tax payable via signed computation")
                                .authorizedBy("CFO Nexus Enterprises")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorized").value(true))
                .andExpect(jsonPath("$.data.authorizationMethod").value("CLIENT_CONSENT"));

        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/status")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("AUTHORIZATION"))
                .andExpect(jsonPath("$.data.authorized").value(true))
                .andExpect(jsonPath("$.data.authorizedBy").value("CFO Nexus Enterprises"))
                .andExpect(jsonPath("$.data.nextActionableStep").value("Filing authorization verified; ready to submit return to statutory portal"));

        // 9. Submission Gate satisfies prerequisites -> SUBMISSION stage (FILED)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/mark-filed")
                        .header("Authorization", "Bearer " + makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MarkWorkflowFiledRequest.builder()
                                .filedDate(LocalDate.now())
                                .acknowledgementNumber("ARN-AA2709260192837")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("FILED"))
                .andExpect(jsonPath("$.data.acknowledgementNumber").value("ARN-AA2709260192837"));

        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/status")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("SUBMISSION"))
                .andExpect(jsonPath("$.data.submitted").value(true))
                .andExpect(jsonPath("$.data.acknowledgementNumber").value("ARN-AA2709260192837"));
    }

    @Test
    @DisplayName("Separation of Duties: Preparer cannot approve their own review")
    void testSeparationOfDuties_PreparerCannotApprove() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID workflowId = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

        // Assign maker as preparer and reviewer as reviewer
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

        // Maker attempts to approve their own work -> Forbidden 403
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder()
                                .approvalNotes("Self-approving my own return")
                                .build())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Filing Authorization Gate: Revocation blocks submission until re-authorized")
    void testRevokeAuthorization_EnforcesSubmissionPrerequisite() throws Exception {
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
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder()
                                .approvalNotes("Approved computation")
                                .build())))
                .andExpect(status().isOk());

        // Authorize filing
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/authorize-filing")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AuthorizeFilingRequest.builder()
                                .authorizationMethod(ComplianceAuthorizationMethod.DSC_SIGN_OFF)
                                .authorizationReference("DSC-AUTH-0012")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorized").value(true));

        // Revoke authorization
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/revoke-authorization")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RevokeAuthorizationRequest.builder()
                                .reason("Client notified of an omitted debit note")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorized").value(false));

        // Filing attempt is now blocked
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/mark-filed")
                        .header("Authorization", "Bearer " + makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MarkWorkflowFiledRequest.builder()
                                .filedDate(LocalDate.now())
                                .acknowledgementNumber("ARN-REVOKED-ATTEMPT")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("filing authorization has not been granted")));
    }

    @Test
    @DisplayName("Cannot authorize unapproved workflow")
    void testCannotAuthorizeUnapprovedWorkflow() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID workflowId = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

        // Attempting to authorize while in CREATED / READY status fails
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/authorize-filing")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AuthorizeFilingRequest.builder()
                                .authorizationMethod(ComplianceAuthorizationMethod.CLIENT_CONSENT)
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Filing authorization can only be granted for approved workflows in READY_FOR_FILING status")));
    }

    @Test
    @DisplayName("Tenant Isolation: Cross-tenant access to QC endpoints is denied")
    void testTenantIsolation_CrossTenantAccessDenied() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID workflowId = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

        // Other organization user attempts to access Org A workflow QC status
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/status")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        // Other organization user attempts to authorize filing for Org A workflow
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/authorize-filing")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AuthorizeFilingRequest.builder()
                                .authorizationMethod(ComplianceAuthorizationMethod.CLIENT_CONSENT)
                                .build())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Phase 31.2 Validation: Unassigned workflow returns NOT_EVALUATED for authority and separation, and blocks progression")
    void testValidation_UnassignedWorkflow_ReturnsNotEvaluatedAndBlocks() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID workflowId = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

        // Validate workflow while unassigned
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.passedChecks").isNumber())
                .andExpect(jsonPath("$.data.failedChecks").isNumber())
                .andExpect(jsonPath("$.data.notEvaluatedChecks").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.blockers", hasItem("A reviewer must be assigned to the compliance workflow before review")))
                .andExpect(jsonPath("$.data.preparationFingerprint").isNotEmpty())
                .andExpect(jsonPath("$.data.correctiveActions", hasItem(containsString("Assign an active manager or practitioner as checker"))));
    }

    @Test
    @DisplayName("Phase 31.2 Validation: Explains non-applicable rules for obligations without statutory registration requirements")
    void testValidation_NonStatutoryObligation_ReturnsNotApplicable() throws Exception {
        // Create an obligation type that is NOT GST, ITR, or TDS (e.g. STATUTORY_AUDIT)
        ComplianceObligationEntity nonGstOb = ComplianceObligationEntity.builder()
                .clientId(client.getId())
                .locationId(location.getId())
                .obligationType(ComplianceObligationType.STATUTORY_AUDIT)
                .title("Annual Statutory Audit FY 2025-26")
                .financialYear("2025-26")
                .period("2026-FY")
                .periodLabel("Financial Year 2025-26")
                .statutoryDueDate(LocalDate.of(2026, 10, 31))
                .internalTargetDate(LocalDate.of(2026, 10, 25))
                .status(ComplianceObligationStatus.IN_PROGRESS)
                .build();
        nonGstOb.setOrganizationId(org.getId());
        nonGstOb = obligationRepository.save(nonGstOb);

        String createResp = mockMvc.perform(post("/api/v1/compliance/obligations/" + nonGstOb.getId() + "/workflow")
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.notApplicableChecks").value(1))
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'STATUTORY_IDENTIFIER')].status", hasItem("NOT_APPLICABLE")));
    }

    @Test
    @DisplayName("Phase 31.2 Privacy & Fingerprint: Identifiers are masked in validation messages and fingerprint is deterministic")
    void testValidation_PrivacyMaskingAndFingerprintFreshness() throws Exception {
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

        // Validate workflow
        String validationResp = mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checks[?(@.checkKey == 'STATUTORY_IDENTIFIER')].message", hasItem(containsString("27******Z5"))))
                .andReturn().getResponse().getContentAsString();

        String fingerprint1 = objectMapper.readTree(validationResp).path("data").path("preparationFingerprint").asText();
        assertThat(fingerprint1).startsWith("chk:").contains(":st:IN_PROGRESS");

        // Submit for review - fingerprint should reflect state progression
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        String validationResp2 = mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String fingerprint2 = objectMapper.readTree(validationResp2).path("data").path("preparationFingerprint").asText();
        assertThat(fingerprint2).contains(":st:UNDER_REVIEW");
        assertThat(fingerprint2).isNotEqualTo(fingerprint1);
    }

    @Test
    @DisplayName("Phase 31.3 Review Findings Lifecycle: Record, Respond, Resolve, Multi-Cycle History, and Approval Gate")
    void testReviewFindings_FullLifecycleAndApprovalGate() throws Exception {
        // 1. Initialize workflow
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

        // Validate first before submit
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        // 2. Maker attempts to add finding -> Forbidden (Four-Eyes / Reviewer only)
        com.taxoryn.module.compliance.qc.dto.CreateReviewFindingRequest findingReq =
                com.taxoryn.module.compliance.qc.dto.CreateReviewFindingRequest.builder()
                        .category("CALCULATION")
                        .severity("BLOCKING")
                        .description("ITC under Rule 42 does not reconcile with purchase register")
                        .correctiveGuidance("Reverse Rs. 14,250 in Table 4(B)(2)")
                        .sourceReference("Table 4(A)(5)")
                        .build();

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/findings")
                        .header("Authorization", "Bearer " + makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(findingReq)))
                .andExpect(status().isForbidden());

        // 3. Reviewer records finding -> OK (Cycle 1)
        String addFindingResp = mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/findings")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(findingReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.reviewCycle").value(1))
                .andExpect(jsonPath("$.data.severity").value("BLOCKING"))
                .andReturn().getResponse().getContentAsString();

        UUID findingId = UUID.fromString(objectMapper.readTree(addFindingResp).path("data").path("id").asText());

        // 4. Reviewer attempts to approve with unresolved blocking finding -> Fails (400)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().notes("Looks okay").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("blocking review finding")));

        // 5. Reviewer requests changes -> advances workflow to reviewCycle = 2, status = CHANGES_REQUIRED
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/request-changes")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RequestWorkflowChangesRequest.builder()
                                .reason("Please resolve ITC discrepancy findings")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("CHANGES_REQUIRED"))
                .andExpect(jsonPath("$.data.reviewCycle").value(2));

        // 6. Preparer validates while finding is still OPEN -> Validation fails due to open blocking finding
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.blockers[0]", containsString("blocking review finding")));

        // 7. Preparer tries to submit review without addressing finding -> Fails (400)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("unresolved blocking review findings")));

        // 8. Preparer responds to finding -> status becomes CORRECTED
        com.taxoryn.module.compliance.qc.dto.RespondToFindingRequest respondReq =
                com.taxoryn.module.compliance.qc.dto.RespondToFindingRequest.builder()
                        .response("Reversed Rs. 14,250 in Table 4(B)(2) as instructed")
                        .build();

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/findings/" + findingId + "/respond")
                        .header("Authorization", "Bearer " + makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(respondReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CORRECTED"))
                .andExpect(jsonPath("$.data.preparerResponse").value("Reversed Rs. 14,250 in Table 4(B)(2) as instructed"));

        // 9. Re-run validation after response -> Validation now passes for resubmission
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));

        // 10. Resubmit review -> Succeeds (UNDER_REVIEW)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("UNDER_REVIEW"));

        // 11. Reviewer attempts to approve while finding is only CORRECTED (not yet RESOLVED) -> Fails (400)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().notes("Premature approval").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("blocking review finding")));

        // 12. Reviewer resolves finding -> status becomes RESOLVED
        com.taxoryn.module.compliance.qc.dto.ResolveFindingRequest resolveReq =
                com.taxoryn.module.compliance.qc.dto.ResolveFindingRequest.builder()
                        .decision("RESOLVED")
                        .reviewerNotes("Reversal verified in revised purchase ledger")
                        .build();

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/findings/" + findingId + "/resolve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolutionDecision").value("RESOLVED"));

        // 13. Reviewer approves -> Succeeds now that blocking findings are RESOLVED
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().notes("All clear").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("READY_FOR_FILING"));

        // 14. Check review cycles audit endpoint
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/review-cycles")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].cycleNumber").value(1))
                .andExpect(jsonPath("$.data[0].totalFindings").value(1))
                .andExpect(jsonPath("$.data[0].unresolvedFindings").value(0));
    }

    @Test
    @DisplayName("Phase 31.3 Validation Freshness: Stale Validation Blocks Submit-Review")
    void testValidationFreshness_StaleValidationBlocksSubmission() throws Exception {
        // Initialize workflow
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

        // 1. Submit review without having run validation -> Fails
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString("Pre-review Quality Control validation has not been executed")));

        // 2. Run validation -> Stores fresh fingerprint
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));

        // 3. Mutate checklist item (completes a step) -> Fingerprint becomes stale
        ComplianceWorkflowEntity wf = workflowRepository.findById(workflowId).orElseThrow();
        var checklistItems = checklistItemRepository.findByWorkflowIdAndOrganizationIdOrderBySequenceOrderAsc(workflowId, org.getId());
        var firstItem = checklistItems.get(0);
        firstItem.setCompleted(true);
        checklistItemRepository.save(firstItem);

        // Submit review -> Fails because computed fingerprint differs from stored lastValidationFingerprint
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString("stale")));

        // 4. Re-run validation to refresh fingerprint
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));

        // 5. Submit review -> Succeeds
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("UNDER_REVIEW"));
    }

    @Test
    @DisplayName("Phase 31.4 Approval Gate: Enforces UNDER_REVIEW status and Four-Eyes separation")
    void testPhase31_4_ApprovalGate_EnforcesUnderReviewStatusAndFourEyes() throws Exception {
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

        // 1. Attempt to approve when IN_PROGRESS -> Fails (400, must be in UNDER_REVIEW)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().notes("Premature").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString("UNDER_REVIEW")));

        // 2. Validate and submit review
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/validate")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/submit-review")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("UNDER_REVIEW"));

        // 3. Maker (preparer) attempts to approve review -> Fails (403, Four-Eyes principle)
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + makerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().notes("Self approval").build())))
                .andExpect(status().isForbidden());

        // 4. Assigned Reviewer approves -> Succeeds
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/approve")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().notes("Verified and approved").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowStatus").value("READY_FOR_FILING"));
    }

    @Test
    @DisplayName("Phase 31.4 Authorization History: Granted and Revoked Immutable Audit Trail")
    void testPhase31_4_AuthorizationHistory_GrantAndRevocationAuditTrail() throws Exception {
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
                        .content(objectMapper.writeValueAsString(ApproveWorkflowRequest.builder().notes("Approved").build())))
                .andExpect(status().isOk());

        // 1. Initial authorization history is empty
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/authorizations")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        // 2. Authorize filing with AUTHORIZED_SIGNATORY
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/authorize-filing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AuthorizeFilingRequest.builder()
                                .authorizationMethod(ComplianceAuthorizationMethod.AUTHORIZED_SIGNATORY)
                                .authorizedBy("Sunita Roy (Director)")
                                .authorizationReference("BOARD-MIN-2026-09")
                                .authorizationNotes("Board resolution authorized statutory return submission")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorized").value(true))
                .andExpect(jsonPath("$.data.authorizationMethod").value("AUTHORIZED_SIGNATORY"));

        // 3. History now has 1 GRANTED record
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/authorizations")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].action").value("GRANTED"))
                .andExpect(jsonPath("$.data[0].authorizationMethod").value("AUTHORIZED_SIGNATORY"))
                .andExpect(jsonPath("$.data[0].authorizedBy").value("Sunita Roy (Director)"))
                .andExpect(jsonPath("$.data[0].authorizationReference").value("BOARD-MIN-2026-09"));

        // 4. Revoke filing authorization with reason
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/qc/revoke-authorization")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RevokeAuthorizationRequest.builder()
                                .reason("Client reported discrepancy in sales credit notes requiring revision")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorized").value(false));

        // 5. History now has 2 records, latest is REVOKED
        mockMvc.perform(get("/api/v1/compliance/workflows/" + workflowId + "/qc/authorizations")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].action").value("REVOKED"))
                .andExpect(jsonPath("$.data[0].revocationReason").value("Client reported discrepancy in sales credit notes requiring revision"))
                .andExpect(jsonPath("$.data[1].action").value("GRANTED"));

        // 6. Attempt to file return while revoked -> Blocked by QC gate
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/mark-filed")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MarkWorkflowFiledRequest.builder()
                                .filedDate(LocalDate.now())
                                .acknowledgementNumber("ARN123456")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString("explicit filing authorization has not been granted")));
    }

    @Test
    @DisplayName("Phase 31.4 Bypass Closure: Direct status updates and completeWorkflow blocked without prerequisites")
    void testPhase31_4_BypassClosure_DirectStatusUpdatesBlocked() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/compliance/obligations/" + obligation.getId() + "/workflow")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID workflowId = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/start")
                        .header("Authorization", "Bearer " + makerToken))
                .andExpect(status().isOk());

        // 1. Attempt to jump directly to READY_FOR_FILING via PUT /status -> Blocked by transition & approval gates
        mockMvc.perform(put("/api/v1/compliance/workflows/" + workflowId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"READY_FOR_FILING\"}"))
                .andExpect(status().isBadRequest());

        // 2. Attempt to jump directly to FILED via PUT /status -> Blocked by transition & submission gates
        mockMvc.perform(put("/api/v1/compliance/workflows/" + workflowId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"FILED\"}"))
                .andExpect(status().isBadRequest());

        // 3. Attempt to jump directly to COMPLETED via PUT /status -> Blocked by transition & submission gates
        mockMvc.perform(put("/api/v1/compliance/workflows/" + workflowId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"COMPLETED\"}"))
                .andExpect(status().isBadRequest());

        // 4. Attempt to complete unfiled workflow via POST /complete -> Blocked by submission gate
        mockMvc.perform(post("/api/v1/compliance/workflows/" + workflowId + "/complete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acknowledgementNumber\": \"ARN999\"}"))
                .andExpect(status().isBadRequest());
    }
}
