package com.taxoryn.module.organization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.organization.dto.UpdateOrganizationRequest;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationEntity.SubscriptionPlan;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrganizationTypeManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UUID userAId;
    private UUID userBId;
    private String orgAdminTokenA;
    private String staffTokenA;
    private String orgAdminTokenB;

    @BeforeEach
    void setUp() {
        // Create Tenant Organization A (Legacy with UNKNOWN)
        orgA = OrganizationEntity.builder()
                .name("Alpha Tax Advisors LLP")
                .legalName("Alpha Tax Advisors LLP")
                .email("contact@alphatax.com")
                .phone("+919876543210")
                .city("Mumbai")
                .state("Maharashtra")
                .country("India")
                .pincode("400001")
                .pan("AAAPA1111A")
                .gstin("27AAAPA1111A1Z5")
                .organizationType(OrganizationType.UNKNOWN)
                .subscriptionPlan(SubscriptionPlan.STARTER)
                .status(OrganizationStatus.ACTIVE)
                .build();
        orgA = organizationRepository.save(orgA);

        // Create Tenant Organization B (Configured with SMALL_TAX_FIRM)
        orgB = OrganizationEntity.builder()
                .name("Beta Tax Solutions LLP")
                .legalName("Beta Tax Solutions LLP")
                .email("contact@betatax.com")
                .phone("+919876543220")
                .city("Bangalore")
                .state("Karnataka")
                .country("India")
                .pincode("560001")
                .pan("BBBPA2222B")
                .gstin("29BBBPA2222B1Z5")
                .organizationType(OrganizationType.SMALL_TAX_FIRM)
                .subscriptionPlan(SubscriptionPlan.PROFESSIONAL)
                .status(OrganizationStatus.ACTIVE)
                .build();
        orgB = organizationRepository.save(orgB);

        userAId = UUID.randomUUID();
        orgAdminTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                userAId,
                orgA.getId(),
                "admin@alphatax.com",
                Set.of("ORG_ADMIN"),
                Set.of("ORGANIZATION_VIEW", "ORGANIZATION_UPDATE", "ORG_READ", "ORG_WRITE")
        );

        staffTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                UUID.randomUUID(),
                orgA.getId(),
                "staff@alphatax.com",
                Set.of("STAFF"),
                Set.of("CLIENT_VIEW", "TASK_VIEW")
        );

        userBId = UUID.randomUUID();
        orgAdminTokenB = "Bearer " + jwtTokenProvider.generateAccessToken(
                userBId,
                orgB.getId(),
                "admin@betatax.com",
                Set.of("ORG_ADMIN"),
                Set.of("ORGANIZATION_VIEW", "ORGANIZATION_UPDATE", "ORG_READ", "ORG_WRITE")
        );
    }

    // ==========================================
    // A. View Tests
    // ==========================================

    @Test
    @DisplayName("1. Authorized Org Admin can view current organization with UNKNOWN legacy type")
    void testViewCurrentOrganization_LegacyUnknown() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current")
                        .header("Authorization", orgAdminTokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.organizationType", is("UNKNOWN")))
                .andExpect(jsonPath("$.data.name", is("Alpha Tax Advisors LLP")));
    }

    @Test
    @DisplayName("2. Authorized Org Admin can view current organization with configured type")
    void testViewCurrentOrganization_ConfiguredType() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current")
                        .header("Authorization", orgAdminTokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.organizationType", is("SMALL_TAX_FIRM")))
                .andExpect(jsonPath("$.data.name", is("Beta Tax Solutions LLP")));
    }

    // ==========================================
    // B. Classification & Update Tests
    // ==========================================

    @ParameterizedTest(name = "Classify UNKNOWN to {0}")
    @EnumSource(value = OrganizationType.class, names = {"SOLO_PRACTITIONER", "SMALL_TAX_FIRM", "GROWING_PRACTICE", "BUSINESS"})
    @DisplayName("3. Classify legacy UNKNOWN organization to each valid supported OrganizationType")
    void testClassifyLegacyUnknownOrganization(OrganizationType targetType) throws Exception {
        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                .name(orgA.getName())
                .legalName(orgA.getLegalName())
                .phone(orgA.getPhone())
                .pan(orgA.getPan())
                .gstin(orgA.getGstin())
                .organizationType(targetType)
                .build();

        mockMvc.perform(put("/api/v1/organizations/current")
                        .header("Authorization", orgAdminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.organizationType", is(targetType.name())));

        OrganizationEntity updated = organizationRepository.findById(orgA.getId()).orElseThrow();
        assertEquals(targetType, updated.getOrganizationType());

        // Verify audit log event
        List<AuditLogEntity> audits = auditLogRepository.findAllByOrganizationId(orgA.getId(), Pageable.unpaged()).getContent();
        boolean auditFound = audits.stream().anyMatch(a ->
                "ORGANIZATION_TYPE_UPDATED".equals(a.getAction()) &&
                "UNKNOWN".equals(a.getOldValue()) &&
                targetType.name().equals(a.getNewValue())
        );
        assertTrue(auditFound, "Audit record for ORGANIZATION_TYPE_UPDATED must be created with previous and new type");
    }

    @Test
    @DisplayName("4. Valid correction from SMALL_TAX_FIRM to GROWING_PRACTICE")
    void testUpdateOrganizationType_SmallTaxFirmToGrowingPractice() throws Exception {
        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                .name(orgB.getName())
                .legalName(orgB.getLegalName())
                .phone(orgB.getPhone())
                .pan(orgB.getPan())
                .gstin(orgB.getGstin())
                .organizationType(OrganizationType.GROWING_PRACTICE)
                .build();

        mockMvc.perform(put("/api/v1/organizations/current")
                        .header("Authorization", orgAdminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.organizationType", is("GROWING_PRACTICE")));

        OrganizationEntity updated = organizationRepository.findById(orgB.getId()).orElseThrow();
        assertEquals(OrganizationType.GROWING_PRACTICE, updated.getOrganizationType());

        // Verify audit log
        List<AuditLogEntity> audits = auditLogRepository.findAllByOrganizationId(orgB.getId(), Pageable.unpaged()).getContent();
        boolean auditFound = audits.stream().anyMatch(a ->
                "ORGANIZATION_TYPE_UPDATED".equals(a.getAction()) &&
                "SMALL_TAX_FIRM".equals(a.getOldValue()) &&
                "GROWING_PRACTICE".equals(a.getNewValue())
        );
        assertTrue(auditFound, "Audit record for transition from SMALL_TAX_FIRM to GROWING_PRACTICE must be recorded");
    }

    // ==========================================
    // C. Tenant Isolation Tests
    // ==========================================

    @Test
    @DisplayName("5. Tenant Isolation: User of Tenant A cannot update Organization B via path parameter")
    void testTenantIsolation_CrossTenantUpdateRejected() throws Exception {
        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                .name("Malicious Hijack Name")
                .organizationType(OrganizationType.BUSINESS)
                .build();

        // Admin A attempts to update Org B
        mockMvc.perform(put("/api/v1/organizations/" + orgB.getId())
                        .header("Authorization", orgAdminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("TENANT_MISMATCH")));

        // Verify Org B remains completely untouched
        OrganizationEntity orgBAfter = organizationRepository.findById(orgB.getId()).orElseThrow();
        assertEquals("Beta Tax Solutions LLP", orgBAfter.getName());
        assertEquals(OrganizationType.SMALL_TAX_FIRM, orgBAfter.getOrganizationType());
    }

    // ==========================================
    // D. Authorization Tests
    // ==========================================

    @Test
    @DisplayName("6. Authorization: Unauthorized staff user cannot update organization type")
    void testAuthorization_UnauthorizedStaffCannotUpdate() throws Exception {
        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                .name(orgA.getName())
                .organizationType(OrganizationType.SOLO_PRACTITIONER)
                .build();

        mockMvc.perform(put("/api/v1/organizations/current")
                        .header("Authorization", staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Verify Org A is unchanged
        OrganizationEntity orgAAfter = organizationRepository.findById(orgA.getId()).orElseThrow();
        assertEquals(OrganizationType.UNKNOWN, orgAAfter.getOrganizationType());
    }

    // ==========================================
    // E. No Side Effects Guarantee
    // ==========================================

    @Test
    @DisplayName("7. No Side Effects: Updating OrganizationType preserves subscription and operational properties")
    void testNoSideEffects_PreservesSubscriptionAndStatus() throws Exception {
        SubscriptionPlan originalPlan = orgB.getSubscriptionPlan();
        OrganizationStatus originalStatus = orgB.getStatus();

        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                .name(orgB.getName())
                .organizationType(OrganizationType.GROWING_PRACTICE)
                .build();

        mockMvc.perform(put("/api/v1/organizations/current")
                        .header("Authorization", orgAdminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        OrganizationEntity updated = organizationRepository.findById(orgB.getId()).orElseThrow();
        assertEquals(OrganizationType.GROWING_PRACTICE, updated.getOrganizationType());
        assertEquals(originalPlan, updated.getSubscriptionPlan(), "Subscription plan must remain unchanged");
        assertEquals(originalStatus, updated.getStatus(), "Organization status must remain unchanged");
    }
}
