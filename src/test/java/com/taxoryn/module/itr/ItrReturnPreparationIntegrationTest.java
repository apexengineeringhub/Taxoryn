package com.taxoryn.module.itr;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.itr.dto.CreateItrProfileRequest;
import com.taxoryn.module.itr.dto.CreateItrReturnRequest;
import com.taxoryn.module.itr.dto.ItrPrepareReturnRequest;
import com.taxoryn.module.itr.dto.ItrPreparedReturnDto;
import com.taxoryn.module.itr.dto.ItrProfileDto;
import com.taxoryn.module.itr.dto.ItrReturnDto;
import com.taxoryn.module.itr.dto.ItrTaxSummaryDto;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ResidentialStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import com.taxoryn.module.itr.integration.ItrGovernmentIntegrationService;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.itr.service.ItrService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class ItrReturnPreparationIntegrationTest {

    @Autowired
    private ItrGovernmentIntegrationService itrGovIntegrationService;

    @Autowired
    private ItrService itrService;

    @Autowired
    private ItrReturnRepository itrReturnRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity testOrg;
    private OrganizationEntity otherOrg;
    private ClientEntity testClient;
    private ItrProfileDto testProfile;
    private ItrReturnDto testReturn;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("ITR Prep Test Practice " + UUID.randomUUID())
                .email("prep.itr." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        otherOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Other ITR Org " + UUID.randomUUID())
                .email("other.itr." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(testOrg.getId());

        SecurityUser testUser = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(testOrg.getId())
                .email("admin@" + testOrg.getId() + ".taxoryn.com")
                .roles(Set.of("ROLE_ORG_ADMIN"))
                .permissions(Set.of("ROLE_ORG_ADMIN", "ITR_VIEW", "ITR_CREATE", "ITR_UPDATE", "ITR_WRITE", "CLIENT_VIEW"))
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities())
        );

        testClient = clientRepository.save(ClientEntity.builder()
                .displayName("Ramesh Kumar Sharma")
                .legalName("Ramesh Kumar Sharma")
                .pan("ABCDE1234F")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.ACTIVE)
                .build());

        testProfile = itrService.createProfile(CreateItrProfileRequest.builder()
                .clientId(testClient.getId())
                .pan("ABCDE1234F")
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .defaultItrType(ItrType.ITR_1)
                .residentialStatus(ResidentialStatus.RESIDENT)
                .build());

        testReturn = itrService.createReturn(CreateItrReturnRequest.builder()
                .clientId(testClient.getId())
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .itrType(ItrType.ITR_1)
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .dueDate(LocalDate.of(2026, 7, 31))
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("prepareReturn with valid inputs produces normalized payload, fingerprint and PREPARED status")
    void testPrepareReturnSuccess() {
        ItrTaxSummaryDto summary = ItrTaxSummaryDto.builder()
                .grossTotalIncome(new BigDecimal("1200000.00"))
                .totalDeductions(new BigDecimal("150000.00"))
                .taxableIncome(new BigDecimal("1050000.00"))
                .taxPayable(new BigDecimal("127500.00"))
                .surcharge(BigDecimal.ZERO)
                .cess(new BigDecimal("5100.00"))
                .totalTaxLiability(new BigDecimal("132600.00"))
                .tdsTcsCredit(new BigDecimal("100000.00"))
                .advanceTaxPaid(new BigDecimal("32600.00"))
                .selfAssessmentTaxPaid(BigDecimal.ZERO)
                .totalTaxesPaid(new BigDecimal("132600.00"))
                .balancePayable(BigDecimal.ZERO)
                .refundDue(BigDecimal.ZERO)
                .build();

        ItrPrepareReturnRequest request = ItrPrepareReturnRequest.builder()
                .pan("ABCDE1234F")
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .returnType("ITR-1")
                .returnId(testReturn.getId())
                .taxSummary(summary)
                .incomeDetails(Map.of("salaryIncome", 1200000))
                .deductions(Map.of("80C", 150000))
                .bankDetails(Map.of("bankName", "State Bank of India", "accountNumber", "1234567890", "ifsc", "SBIN0001234"))
                .build();

        ItrPreparedReturnDto prepared = itrGovIntegrationService.prepareReturn(request);

        assertThat(prepared).isNotNull();
        assertThat(prepared.isReadyForSubmission()).isTrue();
        assertThat(prepared.getStatus()).isEqualTo("PREPARED");
        assertThat(prepared.getPan()).isEqualTo("ABCDE1234F");
        assertThat(prepared.getMaskedPan()).isEqualTo("ABCDE****F");
        assertThat(prepared.getTaxpayerName()).isEqualTo("Ramesh Kumar Sharma");
        assertThat(prepared.getAssessmentYear()).isEqualTo("2026-27");
        assertThat(prepared.getReturnType()).isEqualTo("ITR-1");
        assertThat(prepared.getPayloadFingerprint()).isNotBlank();
        assertThat(prepared.getProviderReferenceId()).startsWith("PREP-ITR-");
        assertThat(prepared.getValidationResult().isValid()).isTrue();

        // Verify ItrReturnEntity transitioned to READY_TO_FILE
        ItrReturnEntity updatedEntity = itrReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(updatedEntity.getStatus()).isEqualTo(ItrStatus.READY_TO_FILE);
    }

    @Test
    @DisplayName("generateFingerprint is strictly deterministic: identical inputs yield identical SHA-256 hash")
    void testDeterministicFingerprintSameInput() {
        ItrTaxSummaryDto summary = ItrTaxSummaryDto.builder()
                .grossTotalIncome(new BigDecimal("900000.00"))
                .totalDeductions(new BigDecimal("100000.00"))
                .taxableIncome(new BigDecimal("800000.00"))
                .build();

        ItrPrepareReturnRequest req1 = ItrPrepareReturnRequest.builder()
                .pan("ABCDE1234F")
                .assessmentYear("2026-27")
                .returnType("ITR-1")
                .taxSummary(summary)
                .incomeDetails(Map.of("salary", 900000))
                .build();

        ItrPrepareReturnRequest req2 = ItrPrepareReturnRequest.builder()
                .pan("abcde1234f")
                .assessmentYear("2026-27")
                .returnType("ITR1")
                .taxSummary(summary)
                .incomeDetails(Map.of("salary", 900000))
                .build();

        ItrPreparedReturnDto prep1 = itrGovIntegrationService.prepareReturn(req1);
        ItrPreparedReturnDto prep2 = itrGovIntegrationService.prepareReturn(req2);

        assertThat(prep1.getPayloadFingerprint()).isEqualTo(prep2.getPayloadFingerprint());
    }

    @Test
    @DisplayName("generateFingerprint changes when financial figures or business data are modified")
    void testFingerprintChangesOnDataModification() {
        ItrTaxSummaryDto summary1 = ItrTaxSummaryDto.builder()
                .grossTotalIncome(new BigDecimal("900000.00"))
                .totalDeductions(new BigDecimal("100000.00"))
                .taxableIncome(new BigDecimal("800000.00"))
                .build();

        ItrTaxSummaryDto summary2 = ItrTaxSummaryDto.builder()
                .grossTotalIncome(new BigDecimal("950000.00"))
                .totalDeductions(new BigDecimal("100000.00"))
                .taxableIncome(new BigDecimal("850000.00"))
                .build();

        ItrPreparedReturnDto prep1 = itrGovIntegrationService.prepareReturn(ItrPrepareReturnRequest.builder()
                .pan("ABCDE1234F")
                .assessmentYear("2026-27")
                .returnType("ITR-1")
                .taxSummary(summary1)
                .build());

        ItrPreparedReturnDto prep2 = itrGovIntegrationService.prepareReturn(ItrPrepareReturnRequest.builder()
                .pan("ABCDE1234F")
                .assessmentYear("2026-27")
                .returnType("ITR-1")
                .taxSummary(summary2)
                .build());

        assertThat(prep1.getPayloadFingerprint()).isNotEqualTo(prep2.getPayloadFingerprint());
    }

    @Test
    @DisplayName("prepareReturn rejects invalid Assessment Year format or non-consecutive span")
    void testInvalidAssessmentYear() {
        ItrPrepareReturnRequest request = ItrPrepareReturnRequest.builder()
                .pan("ABCDE1234F")
                .assessmentYear("2026-28") // non consecutive
                .returnType("ITR-1")
                .build();

        ItrPreparedReturnDto result = itrGovIntegrationService.prepareReturn(request);

        assertThat(result.isReadyForSubmission()).isFalse();
        assertThat(result.getStatus()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.getErrorMessage()).contains("Invalid Assessment Year span");
    }

    @Test
    @DisplayName("prepareReturn rejects unsupported return type")
    void testUnsupportedReturnType() {
        ItrPrepareReturnRequest request = ItrPrepareReturnRequest.builder()
                .pan("ABCDE1234F")
                .assessmentYear("2026-27")
                .returnType("ITR-99")
                .build();

        ItrPreparedReturnDto result = itrGovIntegrationService.prepareReturn(request);

        assertThat(result.isReadyForSubmission()).isFalse();
        assertThat(result.getStatus()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.getErrorMessage()).contains("Unsupported ITR return type");
    }

    @Test
    @DisplayName("prepareReturn rejects incompatible taxpayer category and return form")
    void testIncompatibleTaxpayerCategoryAndForm() {
        ItrPrepareReturnRequest request = ItrPrepareReturnRequest.builder()
                .pan("ABCDE1234F")
                .assessmentYear("2026-27")
                .returnType("ITR-6") // Company form attempted for Individual
                .taxpayerType("INDIVIDUAL")
                .build();

        ItrPreparedReturnDto result = itrGovIntegrationService.prepareReturn(request);

        assertThat(result.isReadyForSubmission()).isFalse();
        assertThat(result.getStatus()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.getErrorMessage()).contains("Taxpayer category INDIVIDUAL is not eligible to file ITR-6");
    }

    @Test
    @DisplayName("prepareReturn detects arithmetic discrepancies in declared tax totals")
    void testArithmeticMismatch() {
        ItrTaxSummaryDto mismatchedSummary = ItrTaxSummaryDto.builder()
                .grossTotalIncome(new BigDecimal("1000000.00"))
                .totalDeductions(new BigDecimal("100000.00"))
                .taxableIncome(new BigDecimal("800000.00")) // Mismatch: 1000000 - 100000 != 800000
                .build();

        ItrPrepareReturnRequest request = ItrPrepareReturnRequest.builder()
                .pan("ABCDE1234F")
                .assessmentYear("2026-27")
                .returnType("ITR-1")
                .taxSummary(mismatchedSummary)
                .build();

        ItrPreparedReturnDto result = itrGovIntegrationService.prepareReturn(request);

        assertThat(result.isReadyForSubmission()).isFalse();
        assertThat(result.getStatus()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.getErrorMessage()).contains("Taxable income arithmetic mismatch");
    }

    @Test
    @DisplayName("prepareReturn emits warnings for missing bank account details")
    void testMissingBankDetailsWarning() {
        ItrPrepareReturnRequest request = ItrPrepareReturnRequest.builder()
                .pan("ABCDE1234F")
                .assessmentYear("2026-27")
                .returnType("ITR-1")
                .bankDetails(Map.of()) // Empty bank details
                .build();

        ItrPreparedReturnDto result = itrGovIntegrationService.prepareReturn(request);

        assertThat(result.getStatus()).isEqualTo("PREPARED");
        assertThat(result.getValidationResult().getWarnings())
                .anyMatch(w -> w.contains("Bank account details"));
    }

    @Test
    @DisplayName("prepareReturn rejects taxpayer PAN not belonging to current organization")
    void testMissingTaxpayerInOrg() {
        ItrPrepareReturnRequest request = ItrPrepareReturnRequest.builder()
                .pan("XYZPA9999Z") // Unknown PAN in testOrg
                .assessmentYear("2026-27")
                .returnType("ITR-1")
                .build();

        ItrPreparedReturnDto result = itrGovIntegrationService.prepareReturn(request);

        assertThat(result.isReadyForSubmission()).isFalse();
        assertThat(result.getStatus()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.getErrorMessage()).contains("was not found for this organization");
    }

    @Test
    @DisplayName("prepareReturn cross-tenant client protection throws TENANT_MISMATCH")
    void testCrossTenantClientProtection() {
        // Create client in other organization
        TenantContext.setTenantId(otherOrg.getId());
        ClientEntity otherClient = clientRepository.save(ClientEntity.builder()
                .displayName("Other Org Client")
                .pan("OTHER1234X")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.ACTIVE)
                .build());

        // Switch back to testOrg and try to prepare with otherClient.getId()
        TenantContext.setTenantId(testOrg.getId());
        ItrPrepareReturnRequest request = ItrPrepareReturnRequest.builder()
                .clientId(otherClient.getId())
                .pan("ABCDE1234F")
                .assessmentYear("2026-27")
                .returnType("ITR-1")
                .build();

        assertThatThrownBy(() -> itrGovIntegrationService.prepareReturn(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Client does not belong to current organization");
    }

    @Test
    @DisplayName("prepareReturn protects FILED returns from re-preparation overwrite")
    void testFiledReturnProtection() {
        ItrReturnEntity filedReturn = itrReturnRepository.save(ItrReturnEntity.builder()
                .clientId(testClient.getId())
                .assessmentYear("2025-26")
                .financialYear("2024-25")
                .itrType(ItrType.ITR_1)
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .status(ItrStatus.FILED)
                .acknowledgementNumber("ACK-123456789")
                .build());

        ItrPrepareReturnRequest request = ItrPrepareReturnRequest.builder()
                .returnId(filedReturn.getId())
                .pan("ABCDE1234F")
                .assessmentYear("2025-26")
                .returnType("ITR-1")
                .build();

        assertThatThrownBy(() -> itrGovIntegrationService.prepareReturn(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Cannot re-prepare an already FILED or COMPLETED ITR return");
    }

    @Test
    @DisplayName("prepareReturn securely masks PAN in audit logs and avoids sensitive leakages")
    @SuppressWarnings("unchecked")
    void testAuditLoggingWithPanMasking() {
        ItrPrepareReturnRequest request = ItrPrepareReturnRequest.builder()
                .pan("ABCDE1234F")
                .assessmentYear("2026-27")
                .returnType("ITR-1")
                .build();

        itrGovIntegrationService.prepareReturn(request);

        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService, atLeastOnce()).logEvent(
                eq(testOrg.getId()),
                any(),
                eq("ITR_RETURN_PREPARED"),
                eq("ITR_RETURN"),
                eq("ABCDE****F"),
                any(),
                detailsCaptor.capture()
        );

        Map<String, Object> details = detailsCaptor.getValue();
        assertThat(details.get("pan")).isEqualTo("ABCDE****F");
        assertThat(details).containsKey("fingerprint");
        assertThat(details).doesNotContainKey("rawSecret");
        assertThat(details).doesNotContainKey("secret");
    }
}
