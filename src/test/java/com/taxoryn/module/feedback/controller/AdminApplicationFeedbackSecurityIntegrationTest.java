package com.taxoryn.module.feedback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.feedback.dto.*;
import com.taxoryn.module.feedback.entity.*;
import com.taxoryn.module.feedback.repository.ApplicationFeedbackRepository;
import com.taxoryn.module.feedback.repository.FeedbackEngineeringIssueRepository;
import com.taxoryn.module.feedback.repository.FeedbackNoteRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
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
class AdminApplicationFeedbackSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ApplicationFeedbackRepository feedbackRepository;

    @Autowired
    private FeedbackEngineeringIssueRepository engineeringIssueRepository;

    @Autowired
    private FeedbackNoteRepository noteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UserEntity superAdminUser;
    private UserEntity practitionerUser;
    private UserEntity employeeUser;
    private UserEntity customerUser;

    private String superAdminToken;
    private String practitionerToken;
    private String employeeToken;
    private String customerToken;

    private OrganizationEntity practice;
    private ApplicationFeedbackEntity testFeedback;

    @BeforeEach
    void setUp() {
        feedbackRepository.deleteAll();
        engineeringIssueRepository.deleteAll();
        noteRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // Roles
        RoleEntity superAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ROLE_SUPER_ADMIN")
                .name("Super Admin")
                .permissions(new HashSet<>())
                .build());

        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ROLE_ORG_ADMIN")
                .name("Practice Admin")
                .permissions(new HashSet<>())
                .build());

        RoleEntity staffRole = roleRepository.save(RoleEntity.builder()
                .code("ROLE_STAFF")
                .name("Staff")
                .permissions(new HashSet<>())
                .build());

        RoleEntity customerRole = roleRepository.save(RoleEntity.builder()
                .code("ROLE_MARKETPLACE_CUSTOMER")
                .name("Customer")
                .permissions(new HashSet<>())
                .build());

        // Practice
        practice = organizationRepository.save(OrganizationEntity.builder()
                .name("Vanguard Tax Services CA")
                .email("info@vanguardca.in")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        // Users
        superAdminUser = userRepository.save(UserEntity.builder()
                .email("admin@taxoryn.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Platform")
                .lastName("SuperAdmin")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(Set.of(superAdminRole))
                .build());
        superAdminToken = jwtTokenProvider.generateAccessToken(superAdminUser.getId(), null, superAdminUser.getEmail(), Set.of("ROLE_SUPER_ADMIN"), Set.of());

        practitionerUser = userRepository.save(UserEntity.builder()
                .organizationId(practice.getId())
                .email("practitioner@vanguardca.in")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Rajeev")
                .lastName("Nath")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(Set.of(orgAdminRole))
                .build());
        practitionerToken = jwtTokenProvider.generateAccessToken(practitionerUser.getId(), practice.getId(), practitionerUser.getEmail(), Set.of("ROLE_ORG_ADMIN"), Set.of());

        employeeUser = userRepository.save(UserEntity.builder()
                .organizationId(practice.getId())
                .email("staff@vanguardca.in")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Anita")
                .lastName("Roy")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(Set.of(staffRole))
                .build());
        employeeToken = jwtTokenProvider.generateAccessToken(employeeUser.getId(), practice.getId(), employeeUser.getEmail(), Set.of("ROLE_STAFF"), Set.of());

        customerUser = userRepository.save(UserEntity.builder()
                .email("client@gmail.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Vikram")
                .lastName("Seth")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(Set.of(customerRole))
                .build());
        customerToken = jwtTokenProvider.generateAccessToken(customerUser.getId(), null, customerUser.getEmail(), Set.of("ROLE_MARKETPLACE_CUSTOMER"), Set.of());

        // Sample Feedback
        testFeedback = feedbackRepository.save(ApplicationFeedbackEntity.builder()
                .userId(practitionerUser.getId())
                .practiceId(practice.getId())
                .actorType(ApplicationFeedbackActorType.PRACTITIONER)
                .contextType(ApplicationFeedbackContextType.PRACTICE_PORTAL)
                .type(ApplicationFeedbackType.PROBLEM)
                .category(ApplicationFeedbackCategory.TAX_SERVICES)
                .rating(1)
                .title("GSTR-9 XML schema validation failing")
                .description("Table 17 HSN summary generates schema format error on export.")
                .page("/gst/returns/annual")
                .feature("GSTR9_EXPORT")
                .status(ApplicationFeedbackStatus.NEW)
                .priority(ApplicationFeedbackPriority.HIGH)
                .build());
    }

    @Test
    @DisplayName("Admin Endpoints: Unauthenticated requests return 401 Unauthorized")
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/admin/feedback"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/feedback/" + testFeedback.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/feedback/" + testFeedback.getId() + "/start-review"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin Endpoints: Non-admin users (Customers, Practitioners, Employees) are rejected with 403 Forbidden")
    void testNonAdminRolesForbidden() throws Exception {
        // 1. Customer
        mockMvc.perform(get("/api/v1/admin/feedback")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // 2. Practitioner (Org Admin)
        mockMvc.perform(get("/api/v1/admin/feedback")
                        .header("Authorization", "Bearer " + practitionerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/feedback/" + testFeedback.getId() + "/start-review")
                        .header("Authorization", "Bearer " + practitionerToken))
                .andExpect(status().isForbidden());

        // 3. Employee
        mockMvc.perform(get("/api/v1/admin/feedback")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Super Admin: Can view feedback list, detail, and KPI statistics")
    void testSuperAdminAccessListAndDetail() throws Exception {
        // List
        mockMvc.perform(get("/api/v1/admin/feedback")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("GSTR-9 XML schema validation failing"))
                .andExpect(jsonPath("$.data.content[0].reporterName").value("Rajeev Nath"))
                .andExpect(jsonPath("$.data.content[0].practiceName").value("Vanguard Tax Services CA"));

        // Detail
        mockMvc.perform(get("/api/v1/admin/feedback/" + testFeedback.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(testFeedback.getId().toString()))
                .andExpect(jsonPath("$.data.reporterEmail").value("practitioner@vanguardca.in"))
                .andExpect(jsonPath("$.data.status").value("NEW"));

        // Stats
        mockMvc.perform(get("/api/v1/admin/feedback/stats")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.newCount").value(1));

        // Teams & Assignees
        mockMvc.perform(get("/api/v1/admin/feedback/teams")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasItem("ENGINEERING")));

        mockMvc.perform(get("/api/v1/admin/feedback/assignees")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Super Admin: Complete workflow lifecycle (Review -> Note -> Priority -> Assign -> Escalate -> Resolve -> Close)")
    void testCompleteAdminFeedbackLifecycle() throws Exception {
        UUID id = testFeedback.getId();

        // 1. Start Review
        mockMvc.perform(post("/api/v1/admin/feedback/" + id + "/start-review")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));

        // 2. Add Internal Note
        CreateFeedbackNoteRequest noteReq = CreateFeedbackNoteRequest.builder()
                .note("Reproduced on sandbox - schema version 1.1 requires additional HSN summary tag.")
                .visibility(FeedbackNoteVisibility.INTERNAL)
                .build();

        mockMvc.perform(post("/api/v1/admin/feedback/" + id + "/notes")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.note").value("Reproduced on sandbox - schema version 1.1 requires additional HSN summary tag."));

        // 3. Update Priority to CRITICAL
        UpdateFeedbackPriorityRequest prioReq = UpdateFeedbackPriorityRequest.builder()
                .priority(ApplicationFeedbackPriority.CRITICAL)
                .reason("Critical for annual GST filing cycle")
                .build();

        mockMvc.perform(patch("/api/v1/admin/feedback/" + id + "/priority")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prioReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priority").value("CRITICAL"));

        // 4. Assign to ENGINEERING
        AssignFeedbackRequest assignReq = AssignFeedbackRequest.builder()
                .team(FeedbackTeam.ENGINEERING)
                .reason("High urgency bug fix")
                .build();

        mockMvc.perform(post("/api/v1/admin/feedback/" + id + "/assign")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.assignedTeam").value("ENGINEERING"));

        // 5. Escalate to Engineering Issue
        EscalateToEngineeringRequest escReq = EscalateToEngineeringRequest.builder()
                .title("GSTR-9 HSN XML Schema Fix")
                .description("Fix JSON to XML serializer to match latest GSTN schema v1.1.")
                .priority(ApplicationFeedbackPriority.CRITICAL)
                .internalNotes("Release with hotfix patch")
                .build();

        mockMvc.perform(post("/api/v1/admin/feedback/" + id + "/escalate")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(escReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.issueCode").value(startsWith("ENG-")))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        // Check feedback status is now ESCALATED
        ApplicationFeedbackEntity updatedFb = feedbackRepository.findById(id).orElseThrow();
        assertThat(updatedFb.getStatus()).isEqualTo(ApplicationFeedbackStatus.ESCALATED);

        // 6. Resolve Feedback
        ResolveFeedbackRequest resReq = ResolveFeedbackRequest.builder()
                .resolutionNote("Deployed hotfix v1.4.1 containing corrected GSTR-9 schema generator.")
                .build();

        mockMvc.perform(post("/api/v1/admin/feedback/" + id + "/resolve")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolutionNote").value("Deployed hotfix v1.4.1 containing corrected GSTR-9 schema generator."));

        // 7. Close Feedback
        CloseFeedbackRequest closeReq = CloseFeedbackRequest.builder()
                .reason("Verified with practitioner")
                .build();

        mockMvc.perform(post("/api/v1/admin/feedback/" + id + "/close")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    @DisplayName("Super Admin: Duplicate linking and Rejection workflows")
    void testDuplicateAndRejectionWorkflows() throws Exception {
        // Feedback 2
        ApplicationFeedbackEntity duplicateFeedback = feedbackRepository.save(ApplicationFeedbackEntity.builder()
                .userId(practitionerUser.getId())
                .practiceId(practice.getId())
                .actorType(ApplicationFeedbackActorType.PRACTITIONER)
                .contextType(ApplicationFeedbackContextType.PRACTICE_PORTAL)
                .type(ApplicationFeedbackType.PROBLEM)
                .category(ApplicationFeedbackCategory.TAX_SERVICES)
                .title("GSTR-9 export bug duplicate")
                .description("Same issue reported again")
                .status(ApplicationFeedbackStatus.NEW)
                .build());

        // Mark as Duplicate
        MarkDuplicateFeedbackRequest dupReq = MarkDuplicateFeedbackRequest.builder()
                .duplicateOfId(testFeedback.getId())
                .reason("Same issue as primary ticket")
                .build();

        mockMvc.perform(post("/api/v1/admin/feedback/" + duplicateFeedback.getId() + "/duplicate")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DUPLICATE"))
                .andExpect(jsonPath("$.data.duplicateOfId").value(testFeedback.getId().toString()));

        // Reject Original Feedback
        RejectFeedbackRequest rejReq = RejectFeedbackRequest.builder()
                .reason("Cannot reproduce issue on current build")
                .build();

        mockMvc.perform(post("/api/v1/admin/feedback/" + testFeedback.getId() + "/reject")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }
}
