package com.taxoryn.module.gst;

import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.ComplianceStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequest;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequestItem;
import com.taxoryn.module.docrequest.dto.DocumentRequestDto;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gst.dto.CreateGstProfileRequest;
import com.taxoryn.module.gst.dto.CreateGstReturnFilingRequest;
import com.taxoryn.module.gst.dto.GstProfileDto;
import com.taxoryn.module.gst.dto.GstReturnFilingDto;
import com.taxoryn.module.gst.dto.UpdateGstFilingStatusRequest;
import com.taxoryn.module.gst.entity.GstProfileEntity.FilingFrequency;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstType;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstReturnType;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.gst.service.GstService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class GstEndToEndWorkflowIntegrationTest {

    @Autowired
    private GstService gstService;

    @Autowired
    private GstProfileRepository gstProfileRepository;

    @Autowired
    private GstReturnFilingRepository gstReturnFilingRepository;

    @Autowired
    private ComplianceObligationRepository complianceObligationRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DocumentRequestRepository documentRequestRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity testOrg;
    private OrganizationEntity orgB;
    private UserEntity adminUser;
    private UserEntity staffUser;
    private EmployeeEntity staffEmployee;
    private ClientEntity testClient;
    private String gstin;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA Practice " + UUID.randomUUID())
                .email("admin-" + UUID.randomUUID() + "@apextax.in")
                .phone("9876543210")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Competitor Practice " + UUID.randomUUID())
                .email("admin-" + UUID.randomUUID() + "@competitortax.in")
                .phone("9876543211")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(testOrg.getId());

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build()));
        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("STAFF").name("Staff").isSystemRole(true).build()));

        adminUser = userRepository.save(UserEntity.builder()
                .email("admin-" + UUID.randomUUID() + "@apextax.in")
                .passwordHash("hashed")
                .firstName("CA Vikram")
                .lastName("Mehta")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .organizationId(testOrg.getId())
                .build());

        staffUser = userRepository.save(UserEntity.builder()
                .email("staff-" + UUID.randomUUID() + "@apextax.in")
                .passwordHash("hashed")
                .firstName("Rahul")
                .lastName("Sharma")
                .roles(new HashSet<>(Set.of(staffRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .organizationId(testOrg.getId())
                .build());

        EmployeeEntity staffEmp = EmployeeEntity.builder()
                .userId(staffUser.getId())
                .employeeCode("EMP-001-" + UUID.randomUUID().toString().substring(0, 5))
                .firstName("Rahul")
                .lastName("Sharma")
                .email(staffUser.getEmail())
                .designation("Senior Tax Associate")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build();
        staffEmp.setOrganizationId(testOrg.getId());
        staffEmployee = employeeRepository.save(staffEmp);

        ClientEntity client = ClientEntity.builder()
                .displayName("Shree Enterprises")
                .legalName("Shree Enterprises Private Limited")
                .pan("AAACS1234D")
                .assignedEmployeeId(staffEmployee.getId())
                .build();
        client.setOrganizationId(testOrg.getId());
        testClient = clientRepository.save(client);

        gstin = "27AAACS1234D1Z5";
        setAuthContext(adminUser, "ORG_ADMIN", "GST_VIEW", "GST_CREATE", "GST_UPDATE", "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void setAuthContext(UserEntity user, String role, String... permissions) {
        Set<String> roles = Set.of(role);
        Set<String> perms = Set.of(permissions);
        SecurityUser securityUser = SecurityUser.builder()
                .userId(user.getId())
                .organizationId(user.getOrganizationId())
                .email(user.getEmail())
                .roles(roles)
                .permissions(perms)
                .enabled(true)
                .build();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(user.getOrganizationId());
    }

    @Test
    @DisplayName("1. Create GST Profile with state code inference and verify client association")
    void testCreateGstProfileWithValidationAndTenantIsolation() {
        CreateGstProfileRequest req = CreateGstProfileRequest.builder()
                .clientId(testClient.getId())
                .gstin(gstin)
                .legalName("Shree Enterprises Private Limited")
                .tradeName("Shree Enterprises")
                .gstType(GstType.REGULAR)
                .filingFrequency(FilingFrequency.MONTHLY)
                .assignedEmployeeId(staffEmployee.getId())
                .build();

        GstProfileDto profile = gstService.createProfile(req);

        assertThat(profile).isNotNull();
        assertThat(profile.getGstin()).isEqualTo(gstin);
        assertThat(profile.getStateCode()).isEqualTo("27");
        assertThat(profile.getClientId()).isEqualTo(testClient.getId());
        assertThat(profile.getAssignedEmployeeId()).isEqualTo(staffEmployee.getId());

        // Verify client was updated with GSTIN
        ClientEntity updatedClient = clientRepository.findByIdAndOrganizationId(testClient.getId(), testOrg.getId()).orElseThrow();
        assertThat(updatedClient.getGstin()).isEqualTo(gstin);
    }

    @Test
    @DisplayName("2. Create GST Filing and verify auto-linkage with statutory Compliance Obligation")
    void testCreateGstFilingAndAutoLinkComplianceObligation() {
        CreateGstProfileRequest profileReq = CreateGstProfileRequest.builder()
                .clientId(testClient.getId())
                .gstin(gstin)
                .assignedEmployeeId(staffEmployee.getId())
                .build();
        GstProfileDto profile = gstService.createProfile(profileReq);

        CreateGstReturnFilingRequest filingReq = CreateGstReturnFilingRequest.builder()
                .gstProfileId(profile.getId())
                .returnType(GstReturnType.GSTR3B)
                .returnPeriod("2026-07")
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 8, 20))
                .totalTaxableValue(new BigDecimal("1500000.00"))
                .totalTaxLiability(new BigDecimal("270000.00"))
                .totalItcClaimed(new BigDecimal("120000.00"))
                .build();

        GstReturnFilingDto filing = gstService.createFiling(filingReq);

        assertThat(filing).isNotNull();
        assertThat(filing.getComplianceId()).isNotNull();
        assertThat(filing.getFilingStatus()).isEqualTo(GstFilingStatus.PENDING);

        // Verify linked Compliance Obligation in DB
        ComplianceObligationEntity obligation = complianceObligationRepository
                .findByIdAndOrganizationId(filing.getComplianceId(), testOrg.getId()).orElseThrow();
        assertThat(obligation.getPeriod()).isEqualTo("2026-07");
        assertThat(obligation.getGstFilingId()).isEqualTo(filing.getId());
        assertThat(obligation.getStatus()).isEqualTo(ComplianceStatus.PENDING);
    }

    @Test
    @DisplayName("3. Create Task for GST Filing and verify Employee ID -> User ID resolution")
    void testCreateTaskForGstFilingWithEmployeeToUserResolution() {
        CreateGstProfileRequest profileReq = CreateGstProfileRequest.builder()
                .clientId(testClient.getId())
                .gstin(gstin)
                .assignedEmployeeId(staffEmployee.getId())
                .build();
        GstProfileDto profile = gstService.createProfile(profileReq);

        CreateGstReturnFilingRequest filingReq = CreateGstReturnFilingRequest.builder()
                .gstProfileId(profile.getId())
                .returnType(GstReturnType.GSTR1)
                .returnPeriod("2026-07")
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 8, 11))
                .build();
        GstReturnFilingDto filing = gstService.createFiling(filingReq);

        // Generate linked Task
        GstReturnFilingDto updatedFiling = gstService.createTaskForFiling(filing.getId());

        assertThat(updatedFiling.getTaskId()).isNotNull();

        // Verify Task in TaskRepository
        TaskEntity task = taskRepository.findByIdAndOrganizationId(updatedFiling.getTaskId(), testOrg.getId()).orElseThrow();
        assertThat(task.getTaskCategory()).isEqualTo(TaskCategory.GST);
        assertThat(task.getTitle()).contains("GSTR1");
        assertThat(task.getGstFilingId()).isEqualTo(filing.getId());
        assertThat(task.getComplianceId()).isEqualTo(filing.getComplianceId());

        // CRITICAL: Verify assignedTo was resolved to User ID, NOT Employee ID!
        assertThat(task.getAssignedTo()).isEqualTo(staffUser.getId());
        assertThat(task.getAssignedTo()).isNotEqualTo(staffEmployee.getId());
    }

    @Test
    @DisplayName("4. Create Document Request for GST Filing and verify item counters")
    void testGstDocumentRequestAndUploadFlow() {
        CreateGstProfileRequest profileReq = CreateGstProfileRequest.builder()
                .clientId(testClient.getId())
                .gstin(gstin)
                .assignedEmployeeId(staffEmployee.getId())
                .build();
        GstProfileDto profile = gstService.createProfile(profileReq);

        CreateGstReturnFilingRequest filingReq = CreateGstReturnFilingRequest.builder()
                .gstProfileId(profile.getId())
                .returnType(GstReturnType.GSTR3B)
                .returnPeriod("2026-07")
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 8, 20))
                .createTask(true)
                .build();
        GstReturnFilingDto filing = gstService.createFiling(filingReq);

        // Create Document Request
        CreateDocumentRequest docReqPayload = CreateDocumentRequest.builder()
                .purpose("GSTR-3B July 2026 Invoices & Registers")
                .items(List.of(
                        CreateDocumentRequestItem.builder().title("July Sales Register Excel").documentType(DocumentType.GST_INVOICE_SALE).required(true).build(),
                        CreateDocumentRequestItem.builder().title("July Purchase Register Excel").documentType(DocumentType.GST_INVOICE_PURCHASE).required(true).build(),
                        CreateDocumentRequestItem.builder().title("GSTR-2B Statement").documentType(DocumentType.OTHER).required(false).build()
                ))
                .build();

        DocumentRequestDto docReq = gstService.createDocumentRequestForFiling(filing.getId(), docReqPayload);

        assertThat(docReq).isNotNull();
        assertThat(docReq.getItems()).hasSize(3);

        // Fetch filing and assert enriched fields
        GstReturnFilingDto enrichedFiling = gstService.getFilingById(filing.getId());
        assertThat(enrichedFiling.getDocumentRequestId()).isEqualTo(docReq.getId());
        assertThat(enrichedFiling.getDocumentRequestItemsCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("5. End-to-End Review Workflow and Recording ARN with full Task & Compliance sync")
    void testReviewWorkflowAndFilingArnRecordingWithFullSync() {
        CreateGstProfileRequest profileReq = CreateGstProfileRequest.builder()
                .clientId(testClient.getId())
                .gstin(gstin)
                .assignedEmployeeId(staffEmployee.getId())
                .build();
        GstProfileDto profile = gstService.createProfile(profileReq);

        CreateGstReturnFilingRequest filingReq = CreateGstReturnFilingRequest.builder()
                .gstProfileId(profile.getId())
                .returnType(GstReturnType.GSTR3B)
                .returnPeriod("2026-07")
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 8, 20))
                .createTask(true)
                .build();
        GstReturnFilingDto filing = gstService.createFiling(filingReq);

        // Step 1: Practitioner prepares return
        UpdateGstFilingStatusRequest prepReq = UpdateGstFilingStatusRequest.builder()
                .filingStatus(GstFilingStatus.PREPARED)
                .totalTaxableValue(new BigDecimal("2000000.00"))
                .totalTaxLiability(new BigDecimal("360000.00"))
                .totalItcClaimed(new BigDecimal("150000.00"))
                .taxPaidCash(new BigDecimal("210000.00"))
                .taxPaidItc(new BigDecimal("150000.00"))
                .build();
        GstReturnFilingDto prepared = gstService.updateFilingStatus(filing.getId(), prepReq);
        assertThat(prepared.getFilingStatus()).isEqualTo(GstFilingStatus.PREPARED);

        // Step 2: Submit for review
        UpdateGstFilingStatusRequest reviewReq = UpdateGstFilingStatusRequest.builder()
                .filingStatus(GstFilingStatus.UNDER_REVIEW)
                .build();
        GstReturnFilingDto underReview = gstService.updateFilingStatus(filing.getId(), reviewReq);
        assertThat(underReview.getFilingStatus()).isEqualTo(GstFilingStatus.UNDER_REVIEW);

        TaskEntity taskDuringReview = taskRepository.findByIdAndOrganizationId(filing.getTaskId(), testOrg.getId()).orElseThrow();
        assertThat(taskDuringReview.getStatus()).isEqualTo(TaskStatus.UNDER_REVIEW);

        // Step 3: Practitioner files on Government Portal and records ARN
        String arn = "AA2708260098765";
        LocalDate fileDate = LocalDate.of(2026, 8, 19);
        UpdateGstFilingStatusRequest filedReq = UpdateGstFilingStatusRequest.builder()
                .filingStatus(GstFilingStatus.FILED)
                .acknowledgementNumber(arn)
                .filingDate(fileDate)
                .build();
        GstReturnFilingDto filed = gstService.updateFilingStatus(filing.getId(), filedReq);

        assertThat(filed.getFilingStatus()).isEqualTo(GstFilingStatus.FILED);
        assertThat(filed.getAcknowledgementNumber()).isEqualTo(arn);
        assertThat(filed.getFilingDate()).isEqualTo(fileDate);

        // Step 4: Verify Compliance Obligation is automatically COMPLETED
        ComplianceObligationEntity completedObligation = complianceObligationRepository
                .findByIdAndOrganizationId(filing.getComplianceId(), testOrg.getId()).orElseThrow();
        assertThat(completedObligation.getStatus()).isEqualTo(ComplianceStatus.COMPLETED);
        assertThat(completedObligation.getCompletedAt()).isNotNull();

        // Step 5: Verify Task is automatically COMPLETED
        TaskEntity completedTask = taskRepository.findByIdAndOrganizationId(filing.getTaskId(), testOrg.getId()).orElseThrow();
        assertThat(completedTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completedTask.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("6. Cross-Tenant Security Isolation: Organization B cannot access Organization A's GST data")
    void testCrossTenantSecurityIsolation() {
        CreateGstProfileRequest profileReq = CreateGstProfileRequest.builder()
                .clientId(testClient.getId())
                .gstin(gstin)
                .assignedEmployeeId(staffEmployee.getId())
                .build();
        GstProfileDto profile = gstService.createProfile(profileReq);

        CreateGstReturnFilingRequest filingReq = CreateGstReturnFilingRequest.builder()
                .gstProfileId(profile.getId())
                .returnType(GstReturnType.GSTR1)
                .returnPeriod("2026-07")
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 8, 11))
                .build();
        GstReturnFilingDto filing = gstService.createFiling(filingReq);

        // Switch security context to Organization B
        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build()));

        UserEntity orgBUser = userRepository.save(UserEntity.builder()
                .email("partner-" + UUID.randomUUID() + "@competitortax.in")
                .passwordHash("hashed")
                .firstName("Anita")
                .lastName("Desai")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .organizationId(orgB.getId())
                .build());

        setAuthContext(orgBUser, "ORG_ADMIN", "GST_VIEW", "GST_UPDATE");

        // Attempting to access Org A's profile or filing from Org B context must fail with ResourceNotFoundException
        assertThrows(Exception.class, () -> gstService.getProfileById(profile.getId()));
        assertThrows(Exception.class, () -> gstService.getFilingById(filing.getId()));
        assertThrows(Exception.class, () -> gstService.updateFilingStatus(filing.getId(),
                UpdateGstFilingStatusRequest.builder().filingStatus(GstFilingStatus.FILED).acknowledgementNumber("HACKED").build()));
    }
}
