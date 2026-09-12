package com.taxoryn.module.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.notice.dto.RecordHearingOutcomeRequest;
import com.taxoryn.module.notice.dto.ReviewNoticeResponseRequest;
import com.taxoryn.module.notice.dto.SubmitNoticeRequest;
import com.taxoryn.module.notice.entity.NoticeHearingEntity;
import com.taxoryn.module.notice.entity.NoticeResponseEntity;
import com.taxoryn.module.notice.entity.TaxNoticeEntity;
import com.taxoryn.module.notice.enums.HearingMode;
import com.taxoryn.module.notice.enums.HearingStatus;
import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.notice.enums.NoticeStatus;
import com.taxoryn.module.notice.enums.ReviewStatus;
import com.taxoryn.module.notice.enums.SubmissionMode;
import com.taxoryn.module.notice.repository.NoticeHearingRepository;
import com.taxoryn.module.notice.repository.NoticeResponseRepository;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NoticeResponseOwnershipSecurityTest {

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
    private ClientRepository clientRepository;

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

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;

    private UserEntity userA1; // Maker in Org A
    private UserEntity userA2; // Checker/Partner in Org A
    private UserEntity userB1; // User in Org B

    private String tokenA1;
    private String tokenA2;
    private String tokenB1;

    private ClientEntity clientA;
    private ClientEntity clientB;

    private TaxNoticeEntity noticeA1;
    private TaxNoticeEntity noticeA2;
    private TaxNoticeEntity noticeB1;

    private NoticeResponseEntity responseA1;
    private NoticeResponseEntity responseA2;
    private NoticeResponseEntity responseB1;

    private NoticeHearingEntity hearingA1;
    private NoticeHearingEntity hearingA2;
    private NoticeHearingEntity hearingB1;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        // 1. Setup Roles
        RoleEntity partnerRole = roleRepository.findByCodeAndIsSystemRoleTrue("PARTNER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("PARTNER")
                        .name("Partner")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("STAFF")
                        .name("Staff")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        // 2. Setup Organizations
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Practice Alpha " + unique)
                .email("alpha." + unique + "@taxoryn.in")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Practice Beta " + unique)
                .email("beta." + unique + "@taxoryn.in")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 3. Setup Users
        userA1 = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("maker.alpha." + unique + "@taxoryn.in")
                .firstName("Maker")
                .lastName("Alpha")
                .passwordHash(passwordEncoder.encode("Tx9#SecureP@ss2026!"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());

        userA2 = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("partner.alpha." + unique + "@taxoryn.in")
                .firstName("Partner")
                .lastName("Alpha")
                .passwordHash(passwordEncoder.encode("Tx9#SecureP@ss2026!"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(partnerRole)))
                .build());

        userB1 = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("user.beta." + unique + "@taxoryn.in")
                .firstName("User")
                .lastName("Beta")
                .passwordHash(passwordEncoder.encode("Tx9#SecureP@ss2026!"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(partnerRole)))
                .build());

        Set<String> noticePermissions = Set.of(
                "NOTICE_VIEW", "NOTICE_CREATE", "NOTICE_UPDATE", "NOTICE_DELETE",
                "NOTICE_RESPONSE_CREATE", "NOTICE_RESPONSE_REVIEW", "NOTICE_APPROVE",
                "NOTICE_SUBMIT", "NOTICE_CLOSE"
        );

        tokenA1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                userA1.getId(), orgA.getId(), userA1.getEmail(),
                Set.of("STAFF"), noticePermissions
        );

        tokenA2 = "Bearer " + jwtTokenProvider.generateAccessToken(
                userA2.getId(), orgA.getId(), userA2.getEmail(),
                Set.of("PARTNER"), noticePermissions
        );

        tokenB1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                userB1.getId(), orgB.getId(), userB1.getEmail(),
                Set.of("PARTNER"), noticePermissions
        );

        // 4. Setup Clients and Notices for Org A
        TenantContext.setTenantId(orgA.getId());

        clientA = clientRepository.save(ClientEntity.builder()
                .displayName("Client Alpha " + unique)
                .pan("ABCDE1234F")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.ACTIVE)
                .build());

        noticeA1 = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(clientA.getId())
                .noticeNumber("NOT-A1-" + unique)
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("Scrutiny Notice u/s 143(2)")
                .section("143(2)")
                .subject("Notice A1 Scrutiny")
                .assessmentYear("2024-25")
                .financialYear("2023-24")
                .demandAmount(new BigDecimal("150000.00"))
                .noticeDate(LocalDate.now().minusDays(5))
                .receivedDate(LocalDate.now().minusDays(4))
                .responseDueDate(LocalDate.now().plusDays(15))
                .status(NoticeStatus.INTERNAL_REVIEW)
                .priority(NoticePriority.HIGH)
                .build());

        noticeA2 = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(clientA.getId())
                .noticeNumber("NOT-A2-" + unique)
                .department(NoticeDepartment.GST)
                .noticeType("DRC-01 Demand Notice")
                .section("73")
                .subject("Notice A2 ITC Mismatch")
                .assessmentYear("2024-25")
                .financialYear("2023-24")
                .demandAmount(new BigDecimal("75000.00"))
                .noticeDate(LocalDate.now().minusDays(3))
                .receivedDate(LocalDate.now().minusDays(2))
                .responseDueDate(LocalDate.now().plusDays(20))
                .status(NoticeStatus.INTERNAL_REVIEW)
                .priority(NoticePriority.MEDIUM)
                .build());

        // Responses for Org A
        responseA1 = responseRepository.save(NoticeResponseEntity.builder()
                .noticeId(noticeA1.getId())
                .responseVersion(1)
                .responseTitle("Draft Response for Notice A1")
                .responseSummary("Reply explaining tax compliance for 143(2)")
                .preparedByUserId(userA1.getId())
                .reviewStatus(ReviewStatus.PENDING_REVIEW)
                .build());

        responseA2 = responseRepository.save(NoticeResponseEntity.builder()
                .noticeId(noticeA2.getId())
                .responseVersion(1)
                .responseTitle("Draft Response for Notice A2")
                .responseSummary("Reply explaining GSTR-2B vs 3B discrepancy")
                .preparedByUserId(userA1.getId())
                .reviewStatus(ReviewStatus.PENDING_REVIEW)
                .build());

        // Hearings for Org A
        hearingA1 = hearingRepository.save(NoticeHearingEntity.builder()
                .noticeId(noticeA1.getId())
                .hearingDate(LocalDate.now().plusDays(10))
                .hearingTime("11:00 AM")
                .hearingMode(HearingMode.VIRTUAL_VC)
                .authorityName("Income Tax Assessment Unit")
                .status(HearingStatus.SCHEDULED)
                .build());

        hearingA2 = hearingRepository.save(NoticeHearingEntity.builder()
                .noticeId(noticeA2.getId())
                .hearingDate(LocalDate.now().plusDays(12))
                .hearingTime("02:30 PM")
                .hearingMode(HearingMode.PHYSICAL)
                .authorityName("State GST Ward 3")
                .status(HearingStatus.SCHEDULED)
                .build());

        // 5. Setup Client and Notice for Org B
        TenantContext.setTenantId(orgB.getId());

        clientB = clientRepository.save(ClientEntity.builder()
                .displayName("Client Beta " + unique)
                .pan("VWXYZ5678G")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.ACTIVE)
                .build());

        noticeB1 = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(clientB.getId())
                .noticeNumber("NOT-B1-" + unique)
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("148 Reopening Notice")
                .section("148")
                .subject("Notice B1 Reopening")
                .assessmentYear("2021-22")
                .financialYear("2020-21")
                .demandAmount(new BigDecimal("500000.00"))
                .noticeDate(LocalDate.now().minusDays(10))
                .receivedDate(LocalDate.now().minusDays(9))
                .responseDueDate(LocalDate.now().plusDays(10))
                .status(NoticeStatus.INTERNAL_REVIEW)
                .priority(NoticePriority.CRITICAL)
                .build());

        responseB1 = responseRepository.save(NoticeResponseEntity.builder()
                .noticeId(noticeB1.getId())
                .responseVersion(1)
                .responseTitle("Draft Response for Notice B1")
                .responseSummary("Challenge against 148 reasons recorded")
                .preparedByUserId(userB1.getId())
                .reviewStatus(ReviewStatus.PENDING_REVIEW)
                .build());

        hearingB1 = hearingRepository.save(NoticeHearingEntity.builder()
                .noticeId(noticeB1.getId())
                .hearingDate(LocalDate.now().plusDays(8))
                .hearingTime("10:00 AM")
                .hearingMode(HearingMode.VIRTUAL_VC)
                .authorityName("ITAT Bench 1")
                .status(HearingStatus.SCHEDULED)
                .build());

        TenantContext.clear();
    }

    // =========================================================================
    // 1. INTRA-TENANT CROSS-NOTICE RESPONSE OWNERSHIP VALIDATION
    // =========================================================================

    @Test
    @DisplayName("SEC-009: Fetching Response A2 using Notice A1 path returns 404 (Intra-tenant cross-notice mismatch)")
    void testGetResponse_CrossNoticeMismatch_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/notices/{noticeId}/responses/{responseId}",
                noticeA1.getId(), responseA2.getId())
                .header("Authorization", tokenA2))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("SEC-009: Reviewing Response A2 using Notice A1 path returns 404 (Intra-tenant cross-notice mismatch)")
    void testReviewResponse_CrossNoticeMismatch_Returns404() throws Exception {
        ReviewNoticeResponseRequest request = new ReviewNoticeResponseRequest();
        request.setAction("APPROVE_PARTNER");
        request.setComments("Unauthorized attempt to review Response A2 via Notice A1");

        mockMvc.perform(post("/api/v1/notices/{noticeId}/responses/{responseId}/review",
                noticeA1.getId(), responseA2.getId())
                .header("Authorization", tokenA2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("SEC-009: Submitting Notice A1 with Response A2 ID returns 404 (Intra-tenant cross-notice mismatch)")
    void testSubmitNotice_CrossNoticeResponseMismatch_Returns404() throws Exception {
        SubmitNoticeRequest request = new SubmitNoticeRequest();
        request.setSubmissionMode(SubmissionMode.INCOME_TAX_PORTAL);
        request.setPortalAcknowledgementNumber("ACK-IT-12345678");
        request.setResponseId(responseA2.getId()); // Belongs to Notice A2, not Notice A1!

        mockMvc.perform(post("/api/v1/notices/{noticeId}/submit", noticeA1.getId())
                .header("Authorization", tokenA2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    // =========================================================================
    // 2. CROSS-TENANT RESPONSE OWNERSHIP VALIDATION
    // =========================================================================

    @Test
    @DisplayName("SEC-009: Fetching Org B's Response B1 using Org A's Notice A1 returns 404")
    void testGetResponse_CrossTenantResponse_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/notices/{noticeId}/responses/{responseId}",
                noticeA1.getId(), responseB1.getId())
                .header("Authorization", tokenA2))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("SEC-009: Reviewing Org B's Response B1 using Org A's token returns 404")
    void testReviewResponse_CrossTenantResponse_Returns404() throws Exception {
        ReviewNoticeResponseRequest request = new ReviewNoticeResponseRequest();
        request.setAction("APPROVE_PARTNER");
        request.setComments("Cross-tenant tampering attempt");

        // Even if attacker passes Org B's noticeId and responseId, Org A token cannot see Org B's notice
        mockMvc.perform(post("/api/v1/notices/{noticeId}/responses/{responseId}/review",
                noticeB1.getId(), responseB1.getId())
                .header("Authorization", tokenA2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("SEC-009: Submitting Notice A1 with Org B's Response B1 returns 404")
    void testSubmitNotice_CrossTenantResponseMismatch_Returns404() throws Exception {
        SubmitNoticeRequest request = new SubmitNoticeRequest();
        request.setSubmissionMode(SubmissionMode.INCOME_TAX_PORTAL);
        request.setPortalAcknowledgementNumber("ACK-CROSS-9999");
        request.setResponseId(responseB1.getId()); // Belongs to Org B!

        mockMvc.perform(post("/api/v1/notices/{noticeId}/submit", noticeA1.getId())
                .header("Authorization", tokenA2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // 3. NOTICE HEARINGS OWNERSHIP VALIDATION
    // =========================================================================

    @Test
    @DisplayName("SEC-009: Recording hearing outcome for Hearing A2 using Notice A1 path returns 404 (Intra-tenant)")
    void testRecordHearingOutcome_CrossNoticeMismatch_Returns404() throws Exception {
        RecordHearingOutcomeRequest request = new RecordHearingOutcomeRequest();
        request.setStatus(HearingStatus.COMPLETED);
        request.setProceedingsSummary("Proceedings conducted");

        mockMvc.perform(put("/api/v1/notices/{noticeId}/hearings/{hearingId}/outcome",
                noticeA1.getId(), hearingA2.getId())
                .header("Authorization", tokenA2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("SEC-009: Recording hearing outcome for Org B's Hearing B1 using Org A returns 404 (Cross-tenant)")
    void testRecordHearingOutcome_CrossTenantMismatch_Returns404() throws Exception {
        RecordHearingOutcomeRequest request = new RecordHearingOutcomeRequest();
        request.setStatus(HearingStatus.COMPLETED);
        request.setProceedingsSummary("Cross tenant proceedings tampering");

        mockMvc.perform(put("/api/v1/notices/{noticeId}/hearings/{hearingId}/outcome",
                noticeA1.getId(), hearingB1.getId())
                .header("Authorization", tokenA2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // 4. VALID OWNERSHIP PASSAGE & MAKER-CHECKER ENFORCEMENT
    // =========================================================================

    @Test
    @DisplayName("SEC-009: Legitimate notice response lookup with matching noticeId + responseId + orgId succeeds")
    void testGetResponse_ValidRelationship_Succeeds() throws Exception {
        mockMvc.perform(get("/api/v1/notices/{noticeId}/responses/{responseId}",
                noticeA1.getId(), responseA1.getId())
                .header("Authorization", tokenA2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(responseA1.getId().toString()))
                .andExpect(jsonPath("$.data.noticeId").value(noticeA1.getId().toString()));
    }

    @Test
    @DisplayName("SEC-009: Legitimate partner review on matching notice response succeeds")
    void testReviewResponse_ValidPartnerApproval_Succeeds() throws Exception {
        ReviewNoticeResponseRequest request = new ReviewNoticeResponseRequest();
        request.setAction("APPROVE_PARTNER");
        request.setComments("Legitimate partner approval for Notice A1 response");

        mockMvc.perform(post("/api/v1/notices/{noticeId}/responses/{responseId}/review",
                noticeA1.getId(), responseA1.getId())
                .header("Authorization", tokenA2) // Partner userA2 (different from maker userA1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED_BY_PARTNER"));
    }

    @Test
    @DisplayName("SEC-009: Preparer attempting to approve own response draft is rejected with 403 Forbidden")
    void testReviewResponse_MakerSelfApproval_RejectedWith403() throws Exception {
        ReviewNoticeResponseRequest request = new ReviewNoticeResponseRequest();
        request.setAction("APPROVE_PARTNER");
        request.setComments("Maker attempting self-approval");

        mockMvc.perform(post("/api/v1/notices/{noticeId}/responses/{responseId}/review",
                noticeA1.getId(), responseA1.getId())
                .header("Authorization", tokenA1) // Maker userA1 who created responseA1
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
