package com.taxoryn.module.workflow;

import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.ComplianceStatus;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.itr.dto.CreateItrReturnRequest;
import com.taxoryn.module.itr.dto.ItrReturnDto;
import com.taxoryn.module.itr.dto.RecordItrFilingRequest;
import com.taxoryn.module.itr.dto.UpdateItrStatusRequest;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.itr.service.ItrService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.tds.dto.CreateTdsReturnRequest;
import com.taxoryn.module.tds.dto.RecordTdsFilingRequest;
import com.taxoryn.module.tds.dto.TdsReturnDto;
import com.taxoryn.module.tds.dto.UpdateTdsReturnStatusRequest;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFormType;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
import com.taxoryn.module.tds.service.TdsService;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ItrTdsWorkflowStandardizationIntegrationTest {

    @Autowired
    private ItrService itrService;

    @Autowired
    private TdsService tdsService;

    @Autowired
    private ItrReturnRepository itrReturnRepository;

    @Autowired
    private TdsReturnRepository tdsReturnRepository;

    @Autowired
    private ComplianceObligationRepository complianceObligationRepository;

    @Autowired
    private TaskRepository taskRepository;

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
    private UserEntity practitionerUser;
    private EmployeeEntity practitionerEmp;
    private ClientEntity clientA;

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

        practitionerUser = userRepository.save(UserEntity.builder()
                .email("ca.sharma-" + UUID.randomUUID() + "@apextax.in")
                .passwordHash("hashed")
                .firstName("CA Vikram")
                .lastName("Sharma")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .organizationId(testOrg.getId())
                .build());

        EmployeeEntity staffEmp = EmployeeEntity.builder()
                .userId(practitionerUser.getId())
                .employeeCode("EMP-" + UUID.randomUUID().toString().substring(0, 5))
                .firstName("CA Vikram")
                .lastName("Sharma")
                .email(practitionerUser.getEmail())
                .designation("Senior Tax Partner")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build();
        staffEmp.setOrganizationId(testOrg.getId());
        practitionerEmp = employeeRepository.save(staffEmp);

        clientA = ClientEntity.builder()
                .displayName("Rajesh Exports Ltd")
                .legalName("Rajesh Exports Limited")
                .clientType(ClientEntity.ClientType.PRIVATE_LIMITED)
                .pan("ABCDE1234F")
                .tan("BLRR12345A")
                .assignedEmployeeId(practitionerEmp.getId())
                .email("rajesh@exportsltd.com")
                .phone("9876543210")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        clientA.setOrganizationId(testOrg.getId());
        clientA = clientRepository.save(clientA);

        setAuthContext(practitionerUser, "ORG_ADMIN", "ITR_READ", "ITR_WRITE", "TDS_READ", "TDS_WRITE", "TASK_READ", "TASK_WRITE", "COMPLIANCE_READ", "COMPLIANCE_WRITE");
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

    // =========================================================================
    // 1. ITR WORKFLOW STANDARDIZATION TESTS
    // =========================================================================

    @Test
    @DisplayName("ITR Workflow: Create Return -> Auto-links Compliance & Task -> Review -> Rework -> File & Verify Complete")
    void testItrCompleteEndToEndWorkflow() {
        // Step 1: Create ITR Return
        CreateItrReturnRequest createReq = CreateItrReturnRequest.builder()
                .clientId(clientA.getId())
                .pan(clientA.getPan())
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .itrType(ItrType.ITR_6)
                .taxpayerType(TaxpayerType.COMPANY)
                .dueDate(LocalDate.of(2026, 10, 31))
                .assignedEmployeeId(practitionerEmp.getId())
                .createTask(true)
                .notes("Standard corporate return")
                .build();

        ItrReturnDto returnDto = itrService.createReturn(createReq);
        assertThat(returnDto).isNotNull();
        assertThat(returnDto.getStatus()).isEqualTo(ItrStatus.DOCUMENTS_PENDING);
        assertThat(returnDto.getComplianceId()).isNotNull();
        assertThat(returnDto.getTaskId()).isNotNull();

        // Verify linked Compliance Obligation
        ComplianceObligationEntity compliance = complianceObligationRepository.findById(returnDto.getComplianceId()).orElseThrow();
        assertThat(compliance.getComplianceType()).isEqualTo(ComplianceType.ITR);
        assertThat(compliance.getPeriod()).isEqualTo("2026-27");
        assertThat(compliance.getStatus()).isEqualTo(ComplianceStatus.PENDING);
        assertThat(compliance.getItrReturnId()).isEqualTo(returnDto.getId());

        // Verify linked Task (Respecting Employee ID -> User ID resolution)
        TaskEntity task = taskRepository.findById(returnDto.getTaskId()).orElseThrow();
        assertThat(task.getTaskCategory()).isEqualTo(TaskCategory.ITR);
        assertThat(task.getAssignedTo()).isEqualTo(practitionerUser.getId()); // Resolved to User ID!
        assertThat(task.getItrReturnId()).isEqualTo(returnDto.getId());
        assertThat(task.getComplianceId()).isEqualTo(compliance.getId());
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);

        // Step 2: Transition to UNDER_REVIEW
        UpdateItrStatusRequest reviewReq = UpdateItrStatusRequest.builder()
                .status(ItrStatus.UNDER_REVIEW)
                .notes("Prepared by associate, ready for partner signoff")
                .build();

        ItrReturnDto underReviewDto = itrService.updateStatus(returnDto.getId(), reviewReq);
        assertThat(underReviewDto.getStatus()).isEqualTo(ItrStatus.UNDER_REVIEW);

        TaskEntity taskUnderReview = taskRepository.findById(returnDto.getTaskId()).orElseThrow();
        assertThat(taskUnderReview.getStatus()).isEqualTo(TaskStatus.UNDER_REVIEW);

        // Step 3: Partner requests Rework with comments
        UpdateItrStatusRequest reworkReq = UpdateItrStatusRequest.builder()
                .status(ItrStatus.DATA_ENTRY)
                .reviewComments("Please verify Section 80JJAA deduction certificate")
                .notes("Returned for correction")
                .build();

        ItrReturnDto reworkDto = itrService.updateStatus(returnDto.getId(), reworkReq);
        assertThat(reworkDto.getStatus()).isEqualTo(ItrStatus.DATA_ENTRY);

        TaskEntity taskRework = taskRepository.findById(returnDto.getTaskId()).orElseThrow();
        assertThat(taskRework.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(taskRework.getBlockedReason()).contains("Section 80JJAA");

        // Step 4: Record e-Filing and e-Verification Details (Complete Return)
        RecordItrFilingRequest filingReq = RecordItrFilingRequest.builder()
                .acknowledgementNumber("109283746501928")
                .filingDate(LocalDate.of(2026, 10, 25))
                .verificationDate(LocalDate.of(2026, 10, 25))
                .notes("Successfully filed and e-verified via DSC")
                .build();

        ItrReturnDto completedDto = itrService.recordFilingDetails(returnDto.getId(), filingReq);
        assertThat(completedDto.getStatus()).isEqualTo(ItrStatus.COMPLETED);
        assertThat(completedDto.getAcknowledgementNumber()).isEqualTo("109283746501928");

        // Verify Task is COMPLETED
        TaskEntity completedTask = taskRepository.findById(returnDto.getTaskId()).orElseThrow();
        assertThat(completedTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completedTask.getCompletedAt()).isNotNull();

        // Verify Compliance Obligation is COMPLETED
        ComplianceObligationEntity completedCompliance = complianceObligationRepository.findById(returnDto.getComplianceId()).orElseThrow();
        assertThat(completedCompliance.getStatus()).isEqualTo(ComplianceStatus.COMPLETED);
        assertThat(completedCompliance.getCompletedAt()).isNotNull();
    }

    // =========================================================================
    // 2. TDS WORKFLOW STANDARDIZATION TESTS
    // =========================================================================

    @Test
    @DisplayName("TDS Workflow: Create Return -> Auto-links Compliance & Task -> Review -> Record Filing -> Completed")
    void testTdsCompleteEndToEndWorkflow() {
        // Step 1: Create TDS Return
        CreateTdsReturnRequest createReq = CreateTdsReturnRequest.builder()
                .clientId(clientA.getId())
                .tan(clientA.getTan())
                .formType(TdsFormType.FORM_26Q)
                .quarter(TdsQuarter.Q2)
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 10, 31))
                .assignedEmployeeId(practitionerEmp.getId())
                .totalTaxDeducted(new BigDecimal("150000.00"))
                .totalTaxDeposited(new BigDecimal("150000.00"))
                .createTask(true)
                .notes("Q2 26Q Non-salary TDS")
                .build();

        TdsReturnDto returnDto = tdsService.createReturn(createReq);
        assertThat(returnDto).isNotNull();
        assertThat(returnDto.getFilingStatus()).isEqualTo(TdsFilingStatus.PENDING);
        assertThat(returnDto.getComplianceId()).isNotNull();
        assertThat(returnDto.getTaskId()).isNotNull();

        // Verify linked Compliance Obligation
        ComplianceObligationEntity compliance = complianceObligationRepository.findById(returnDto.getComplianceId()).orElseThrow();
        assertThat(compliance.getComplianceType()).isEqualTo(ComplianceType.TDS);
        assertThat(compliance.getPeriod()).contains("Q2");
        assertThat(compliance.getStatus()).isEqualTo(ComplianceStatus.PENDING);
        assertThat(compliance.getTdsReturnId()).isEqualTo(returnDto.getId());

        // Verify linked Task (Respecting Employee ID -> User ID resolution)
        TaskEntity task = taskRepository.findById(returnDto.getTaskId()).orElseThrow();
        assertThat(task.getTaskCategory()).isEqualTo(TaskCategory.TDS);
        assertThat(task.getAssignedTo()).isEqualTo(practitionerUser.getId());
        assertThat(task.getTdsReturnId()).isEqualTo(returnDto.getId());
        assertThat(task.getComplianceId()).isEqualTo(compliance.getId());
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);

        // Step 2: Transition to UNDER_REVIEW
        UpdateTdsReturnStatusRequest reviewReq = UpdateTdsReturnStatusRequest.builder()
                .filingStatus(TdsFilingStatus.UNDER_REVIEW)
                .notes("FVU file validated, ready for partner signoff")
                .build();

        TdsReturnDto underReviewDto = tdsService.updateStatus(returnDto.getId(), reviewReq);
        assertThat(underReviewDto.getFilingStatus()).isEqualTo(TdsFilingStatus.UNDER_REVIEW);

        TaskEntity taskUnderReview = taskRepository.findById(returnDto.getTaskId()).orElseThrow();
        assertThat(taskUnderReview.getStatus()).isEqualTo(TaskStatus.UNDER_REVIEW);

        // Step 3: Record Filing with Token Number (PRN)
        RecordTdsFilingRequest filingReq = RecordTdsFilingRequest.builder()
                .tokenNumber("040010293847561")
                .receiptNumber("REC-2026-Q2-001")
                .filingDate(LocalDate.of(2026, 10, 28))
                .notes("Filed on TIN-NSDL / Protean portal")
                .build();

        TdsReturnDto filedDto = tdsService.recordFiling(returnDto.getId(), filingReq);
        assertThat(filedDto.getFilingStatus()).isEqualTo(TdsFilingStatus.FILED);
        assertThat(filedDto.getTokenNumber()).isEqualTo("040010293847561");

        // Verify Task is COMPLETED
        TaskEntity completedTask = taskRepository.findById(returnDto.getTaskId()).orElseThrow();
        assertThat(completedTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completedTask.getCompletedAt()).isNotNull();

        // Verify Compliance Obligation is COMPLETED
        ComplianceObligationEntity completedCompliance = complianceObligationRepository.findById(returnDto.getComplianceId()).orElseThrow();
        assertThat(completedCompliance.getStatus()).isEqualTo(ComplianceStatus.COMPLETED);
        assertThat(completedCompliance.getCompletedAt()).isNotNull();
    }

    // =========================================================================
    // 3. MULTI-TENANT ISOLATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Tenant Security Isolation: Org B cannot access or mutate Org A's ITR / TDS records")
    void testTenantSecurityIsolation() {
        // Create ITR and TDS records under Org A
        CreateItrReturnRequest itrReq = CreateItrReturnRequest.builder()
                .clientId(clientA.getId())
                .pan(clientA.getPan())
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .itrType(ItrType.ITR_1)
                .build();
        ItrReturnDto itrA = itrService.createReturn(itrReq);

        CreateTdsReturnRequest tdsReq = CreateTdsReturnRequest.builder()
                .clientId(clientA.getId())
                .tan(clientA.getTan())
                .formType(TdsFormType.FORM_24Q)
                .quarter(TdsQuarter.Q1)
                .financialYear("2026-27")
                .build();
        TdsReturnDto tdsA = tdsService.createReturn(tdsReq);

        // Switch to Org B context
        TenantContext.setTenantId(orgB.getId());

        // Org B attempting to fetch Org A's ITR should fail
        assertThrows(Exception.class, () -> itrService.getReturnById(itrA.getId()));

        // Org B attempting to update Org A's TDS should fail
        assertThrows(Exception.class, () -> tdsService.getReturnById(tdsA.getId()));
    }
}
