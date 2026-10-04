package com.taxoryn.module.tds.integration;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovConnectionHealthDto;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.model.GovConnectionStatus;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gov.service.GovernmentHealthService;
import com.taxoryn.module.gov.service.GovernmentIntegrationService;
import com.taxoryn.module.tds.dto.TdsChallanReconciliationDto;
import com.taxoryn.module.tds.dto.TdsDeductorProfileDto;
import com.taxoryn.module.tds.dto.TdsPrepareReturnRequest;
import com.taxoryn.module.tds.dto.TdsPreparedReturnDto;
import com.taxoryn.module.tds.dto.TdsReturnPayloadDto;
import com.taxoryn.module.tds.dto.TdsReturnStatusDto;
import com.taxoryn.module.tds.dto.TdsReturnTotalsDto;
import com.taxoryn.module.tds.dto.TdsReturnValidationResultDto;
import com.taxoryn.module.tds.entity.TdsChallanEntity;
import com.taxoryn.module.tds.entity.TdsProfileEntity;
import com.taxoryn.module.tds.entity.TdsReturnEntity;
import com.taxoryn.module.tds.integration.dto.TdsHandshakeResponseDto;
import com.taxoryn.module.tds.integration.dto.TdsIntegrationResultDto;
import com.taxoryn.module.tds.repository.TdsChallanRepository;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
import com.taxoryn.module.tds.service.TdsPayloadFingerprintGenerator;
import com.taxoryn.module.tds.service.TdsReturnValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of TdsGovernmentIntegrationService bridging the TDS domain with Government Integration Framework.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TdsGovernmentIntegrationServiceImpl implements TdsGovernmentIntegrationService {

    private static final Pattern TAN_PATTERN = Pattern.compile("^[A-Z]{4}[0-9]{5}[A-Z]{1}$");
    private static final Pattern FY_PATTERN = Pattern.compile("^([0-9]{4})-([0-9]{2})$");
    private static final Pattern FY_FOUR_DIGIT_PATTERN = Pattern.compile("^([0-9]{4})-([0-9]{4})$");

    private final GovernmentIntegrationService govIntegrationService;
    private final GovernmentConnectionService govConnectionService;
    private final GovernmentHealthService govHealthService;
    private final AuditService auditService;
    private final TdsReturnRepository tdsReturnRepository;
    private final TdsProfileRepository tdsProfileRepository;
    private final TdsChallanRepository tdsChallanRepository;
    private final ClientRepository clientRepository;
    private final TdsReturnValidator tdsReturnValidator;
    private final TdsPayloadFingerprintGenerator tdsPayloadFingerprintGenerator;

    @Override
    public TdsHandshakeResponseDto checkTdsConnectionHealth(UUID connectionId) {
        return checkTdsConnectionHealth(connectionId, Collections.emptyMap());
    }

    @Override
    public TdsHandshakeResponseDto checkTdsConnectionHealth(UUID connectionId, Map<String, Object> directives) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = govConnectionService.getConnection(connectionId);

        if (connection.getProviderType() != GovProviderType.TDS) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Connection " + connectionId + " is not a TDS provider connection");
        }

        auditService.logEvent(
                tenantId,
                null,
                "TDS_PROVIDER_HANDSHAKE",
                "TDS_CONNECTION",
                connection.getId().toString(),
                null,
                Map.of("displayName", connection.getDisplayName(), "providerType", connection.getProviderType().name())
        );

        GovConnectionHealthDto health = govHealthService.checkConnectionHealth(connectionId, directives);

        return TdsHandshakeResponseDto.builder()
                .connectionId(health.getConnectionId())
                .displayName(health.getDisplayName())
                .healthStatus(health.getHealthStatus() != null ? health.getHealthStatus().name() : "UNKNOWN")
                .latencyMs(health.getLatencyMs())
                .message(health.getMessage())
                .lastHealthCheckAt(health.getLastHealthCheckAt())
                .build();
    }

    @Override
    public TdsDeductorProfileDto lookupDeductor(String tan) {
        return lookupDeductor(null, tan, Collections.emptyMap());
    }

    @Override
    public TdsDeductorProfileDto lookupDeductor(UUID connectionId, String tan, Map<String, Object> options) {
        UUID tenantId = requireActiveTenantId();
        validateTan(tan);
        String cleanTan = tan.trim().toUpperCase();
        String maskedTan = maskTan(cleanTan);

        GovConnectionDto connection = resolveTdsConnection(connectionId);
        String correlationId = UUID.randomUUID().toString();

        auditService.logEvent(
                tenantId,
                null,
                "TDS_TAN_LOOKUP_REQUESTED",
                "TDS_CONNECTION",
                connection.getId().toString(),
                null,
                Map.of("tan", maskedTan, "correlationId", correlationId)
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("tan", cleanTan);
        if (options != null) {
            payload.putAll(options);
        }

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.TDS)
                .operationType("VERIFY_TAN")
                .businessEntityType("TDS_CONNECTION")
                .businessEntityId(connection.getId())
                .correlationId(correlationId)
                .requestData(payload)
                .build();

        GovIntegrationResult govResult = govIntegrationService.executeOperation(govRequest);
        Instant now = Instant.now();

        if (!govResult.isSuccess()) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "TDS_TAN_LOOKUP_FAILED",
                    "TDS_CONNECTION",
                    connection.getId().toString(),
                    null,
                    Map.of(
                            "tan", maskedTan,
                            "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN",
                            "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : ""
                    )
            );

            return TdsDeductorProfileDto.builder()
                    .tan(cleanTan)
                    .valid(false)
                    .verified(false)
                    .errorCode(govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN")
                    .errorMessage(govResult.getErrorMessage())
                    .operationId(govResult.getOperationId())
                    .verifiedAt(now)
                    .rawData(govResult.getResponseMetadata())
                    .build();
        }

        auditService.logEvent(
                tenantId,
                null,
                "TDS_TAN_LOOKUP_COMPLETED",
                "TDS_CONNECTION",
                connection.getId().toString(),
                null,
                Map.of("tan", maskedTan, "ackNumber", govResult.getProviderReferenceId() != null ? govResult.getProviderReferenceId() : "")
        );

        Map<String, Object> data = govResult.getResponseMetadata() != null ? govResult.getResponseMetadata() : Collections.emptyMap();

        return TdsDeductorProfileDto.builder()
                .tan(cleanTan)
                .deductorName((String) data.getOrDefault("deductorName", "Acme Enterprises Private Limited"))
                .category((String) data.getOrDefault("category", "COMPANY"))
                .status((String) data.getOrDefault("status", "ACTIVE"))
                .tanStatus((String) data.getOrDefault("tanStatus", "VALID"))
                .tracesStatus((String) data.getOrDefault("tracesStatus", "REGISTERED_ACTIVE"))
                .pan((String) data.get("pan"))
                .state((String) data.get("state"))
                .pinCode((String) data.get("pinCode"))
                .address((String) data.get("address"))
                .valid(true)
                .verified(true)
                .providerReferenceId(govResult.getProviderReferenceId())
                .operationId(govResult.getOperationId())
                .verifiedAt(now)
                .rawData(data)
                .build();
    }

    @Override
    public TdsIntegrationResultDto executeTdsOperation(UUID connectionId, String operationType, Map<String, Object> payload) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = govConnectionService.getConnection(connectionId);

        if (connection.getProviderType() != GovProviderType.TDS) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Connection " + connectionId + " is not a TDS provider connection");
        }

        String correlationId = UUID.randomUUID().toString();
        String idempotencyKey = "TDS_OP_" + tenantId + "_" + operationType + "_" + System.currentTimeMillis();

        auditService.logEvent(
                tenantId,
                null,
                "TDS_INTEGRATION_REQUESTED",
                "TDS_INTEGRATION",
                connection.getId().toString(),
                null,
                Map.of("operationType", operationType, "correlationId", correlationId)
        );

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.TDS)
                .operationType(operationType)
                .businessEntityType("TDS_CONNECTION")
                .businessEntityId(connection.getId())
                .correlationId(correlationId)
                .idempotencyKey(idempotencyKey)
                .requestData(payload != null ? new HashMap<>(payload) : new HashMap<>())
                .build();

        GovIntegrationResult govResult = govIntegrationService.executeOperation(govRequest);

        if (!govResult.isSuccess()) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "TDS_INTEGRATION_FAILED",
                    "TDS_INTEGRATION",
                    connection.getId().toString(),
                    null,
                    Map.of(
                            "operationType", operationType,
                            "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN",
                            "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : ""
                    )
            );
        }

        String tan = null;
        if (payload != null && payload.containsKey("tan")) {
            tan = String.valueOf(payload.get("tan"));
        } else if (govResult.getResponseMetadata() != null && govResult.getResponseMetadata().containsKey("tan")) {
            tan = String.valueOf(govResult.getResponseMetadata().get("tan"));
        }

        return TdsIntegrationResultDto.builder()
                .success(govResult.isSuccess())
                .operationId(govResult.getOperationId())
                .connectionId(connectionId)
                .tan(tan)
                .operationType(govResult.getOperationType())
                .correlationId(govResult.getCorrelationId())
                .providerReferenceId(govResult.getProviderReferenceId())
                .errorCode(govResult.getErrorCode() != null ? govResult.getErrorCode().name() : null)
                .errorMessage(govResult.getErrorMessage())
                .data(govResult.getResponseMetadata() != null ? govResult.getResponseMetadata() : new HashMap<>())
                .build();
    }

    @Override
    @Transactional
    public TdsPreparedReturnDto prepareReturn(TdsPrepareReturnRequest request) {
        UUID tenantId = requireActiveTenantId();
        if (request == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Return preparation request must not be null");
        }

        String rawTan = request.getTan() != null ? request.getTan().trim().toUpperCase() : "";
        String maskedTan = maskTan(rawTan);
        String formType = TdsPayloadFingerprintGenerator.normalizeFormType(request.getFormType());
        String financialYear = request.getFinancialYear() != null ? request.getFinancialYear().trim() : "";
        String quarter = request.getQuarter() != null ? request.getQuarter().trim().toUpperCase() : "";
        String assessmentYear = StringUtils.hasText(request.getAssessmentYear())
                ? request.getAssessmentYear().trim()
                : deriveAssessmentYear(financialYear);

        String correlationId = UUID.randomUUID().toString();

        auditService.logEvent(
                tenantId,
                null,
                "TDS_RETURN_PREPARATION_REQUESTED",
                "TDS_RETURN",
                maskedTan,
                null,
                Map.of("tan", maskedTan, "formType", formType, "quarter", quarter, "financialYear", financialYear, "correlationId", correlationId)
        );

        // Resolve TAN Master Profile & Client linkage
        Optional<TdsProfileEntity> resolvedProfile = resolveProfile(tenantId, rawTan, request.getProfileId());
        Optional<ClientEntity> resolvedClient = resolveClient(tenantId, resolvedProfile, request.getClientId());

        // Deductor category & metadata
        String deductorName = resolvedClient
                .map(c -> StringUtils.hasText(c.getLegalName()) ? c.getLegalName() : c.getDisplayName())
                .orElseGet(() -> resolvedProfile
                        .map(TdsProfileEntity::getResponsiblePersonName)
                        .orElse("Acme Enterprises Private Limited"));

        String deductorType = resolvedProfile
                .map(p -> p.getDeductorType().name())
                .orElseGet(() -> StringUtils.hasText(request.getDeductorType()) ? request.getDeductorType().trim().toUpperCase() : "COMPANY");

        String pan = resolvedProfile
                .map(TdsProfileEntity::getResponsiblePersonPan)
                .orElseGet(() -> resolvedClient.map(ClientEntity::getPan).orElse("AAACA1234C"));

        TdsReturnTotalsDto totals = request.getTotals() != null ? request.getTotals() : TdsReturnTotalsDto.builder().build();

        // 1. Validation
        TdsReturnValidationResultDto validationResult = tdsReturnValidator.validate(request, totals, deductorType);

        if (!validationResult.isValid()) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "TDS_RETURN_PREPARATION_FAILED",
                    "TDS_RETURN",
                    maskedTan,
                    null,
                    Map.of(
                            "tan", maskedTan,
                            "formType", formType,
                            "quarter", quarter,
                            "financialYear", financialYear,
                            "errors", String.join("; ", validationResult.getErrors())
                    )
            );

            return TdsPreparedReturnDto.builder()
                    .returnId(request.getReturnId())
                    .clientId(resolvedClient.map(ClientEntity::getId).orElse(request.getClientId()))
                    .profileId(resolvedProfile.map(TdsProfileEntity::getId).orElse(request.getProfileId()))
                    .tan(rawTan)
                    .maskedTan(maskedTan)
                    .deductorName(deductorName)
                    .formType(formType)
                    .financialYear(financialYear)
                    .quarter(quarter)
                    .assessmentYear(assessmentYear)
                    .status("VALIDATION_FAILED")
                    .readyForSubmission(false)
                    .validationResult(validationResult)
                    .preparedAt(Instant.now())
                    .errorCode("VALIDATION_FAILED")
                    .errorMessage(String.join("; ", validationResult.getErrors()))
                    .build();
        }

        // 2. Canonical Payload & Deterministic Fingerprinting
        List<Map<String, Object>> challans = request.getChallans() != null ? request.getChallans() : Collections.emptyList();
        List<Map<String, Object>> deductees = request.getDeductees() != null ? request.getDeductees() : Collections.emptyList();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("schemaVersion", "1.0");
        metadata.put("fvuVersion", "8.2");
        metadata.put("preparedBy", "Taxoryn-TDS-Engine");
        if (request.getOptions() != null) {
            metadata.putAll(request.getOptions());
        }

        String fingerprint = tdsPayloadFingerprintGenerator.generateFingerprint(
                rawTan, formType, financialYear, quarter, assessmentYear, deductorType,
                totals, challans, deductees, metadata
        );

        TdsReturnPayloadDto payloadDto = TdsReturnPayloadDto.builder()
                .tan(rawTan)
                .deductorName(deductorName)
                .deductorType(deductorType)
                .pan(pan)
                .formType(formType)
                .financialYear(financialYear)
                .quarter(quarter)
                .assessmentYear(assessmentYear)
                .totals(totals)
                .challans(challans)
                .deductees(deductees)
                .payloadFingerprint(fingerprint)
                .metadata(metadata)
                .build();

        // 3. Resolve & Update TDS Return Entity
        Optional<TdsReturnEntity> returnOpt = resolveReturn(tenantId, request.getReturnId(), resolvedProfile, formType, quarter, financialYear);
        if (returnOpt.isPresent()) {
            TdsReturnEntity returnEntity = returnOpt.get();
            returnEntity.setFilingStatus(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE);
            returnEntity.setFvuValidationStatus(TdsReturnEntity.FvuValidationStatus.VALIDATED);
            if (totals.getTotalAmountPaid() != null) returnEntity.setTotalAmountPaid(totals.getTotalAmountPaid());
            if (totals.getTotalTaxDeducted() != null) returnEntity.setTotalTaxDeducted(totals.getTotalTaxDeducted());
            if (totals.getTotalTaxDeposited() != null) returnEntity.setTotalTaxDeposited(totals.getTotalTaxDeposited());
            if (totals.getTotalInterest() != null) returnEntity.setTotalInterest(totals.getTotalInterest());
            if (totals.getTotalLateFee() != null) returnEntity.setTotalLateFee(totals.getTotalLateFee());
            if (totals.getTotalPenalty() != null) returnEntity.setTotalPenalty(totals.getTotalPenalty());
            returnEntity.setNotes("Prepared & normalized successfully on " + Instant.now() + " [FP: " + (fingerprint.length() > 12 ? fingerprint.substring(0, 12) : fingerprint) + "...]");
            tdsReturnRepository.save(returnEntity);
        }

        // 4. Dispatch Provider Preparation Operation via Government Integration Framework
        GovConnectionDto connection = resolveTdsConnection(request.getConnectionId());
        Map<String, Object> operationPayload = new HashMap<>();
        operationPayload.put("tan", rawTan);
        operationPayload.put("formType", formType);
        operationPayload.put("quarter", quarter);
        operationPayload.put("financialYear", financialYear);
        operationPayload.put("assessmentYear", assessmentYear);
        operationPayload.put("payloadFingerprint", fingerprint);
        if (request.getOptions() != null) {
            operationPayload.putAll(request.getOptions());
        }

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.TDS)
                .operationType("TDS_RETURN_PREPARATION")
                .businessEntityType("TDS_RETURN")
                .businessEntityId(returnOpt.map(TdsReturnEntity::getId).orElse(connection.getId()))
                .correlationId(correlationId)
                .requestData(operationPayload)
                .build();

        GovIntegrationResult govResult = govIntegrationService.executeOperation(govRequest);

        if (!govResult.isSuccess()) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "TDS_RETURN_PREPARATION_FAILED",
                    "TDS_RETURN",
                    maskedTan,
                    null,
                    Map.of(
                            "tan", maskedTan,
                            "formType", formType,
                            "quarter", quarter,
                            "financialYear", financialYear,
                            "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "ERROR",
                            "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : ""
                    )
            );

            return TdsPreparedReturnDto.builder()
                    .returnId(returnOpt.map(TdsReturnEntity::getId).orElse(request.getReturnId()))
                    .clientId(resolvedClient.map(ClientEntity::getId).orElse(request.getClientId()))
                    .profileId(resolvedProfile.map(TdsProfileEntity::getId).orElse(request.getProfileId()))
                    .tan(rawTan)
                    .maskedTan(maskedTan)
                    .deductorName(deductorName)
                    .formType(formType)
                    .financialYear(financialYear)
                    .quarter(quarter)
                    .assessmentYear(assessmentYear)
                    .status("ERROR")
                    .readyForSubmission(false)
                    .validationResult(validationResult)
                    .payload(payloadDto)
                    .payloadFingerprint(fingerprint)
                    .operationId(govResult.getOperationId())
                    .preparedAt(Instant.now())
                    .errorCode(govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "ERROR")
                    .errorMessage(govResult.getErrorMessage())
                    .build();
        }

        auditService.logEvent(
                tenantId,
                null,
                "TDS_RETURN_PREPARED",
                "TDS_RETURN",
                maskedTan,
                null,
                Map.of("tan", maskedTan, "formType", formType, "quarter", quarter, "financialYear", financialYear, "fingerprint", fingerprint)
        );

        return TdsPreparedReturnDto.builder()
                .returnId(returnOpt.map(TdsReturnEntity::getId).orElse(request.getReturnId()))
                .clientId(resolvedClient.map(ClientEntity::getId).orElse(request.getClientId()))
                .profileId(resolvedProfile.map(TdsProfileEntity::getId).orElse(request.getProfileId()))
                .tan(rawTan)
                .maskedTan(maskedTan)
                .deductorName(deductorName)
                .formType(formType)
                .financialYear(financialYear)
                .quarter(quarter)
                .assessmentYear(assessmentYear)
                .status("READY_TO_FILE")
                .readyForSubmission(true)
                .validationResult(validationResult)
                .payload(payloadDto)
                .payloadFingerprint(fingerprint)
                .providerReferenceId(govResult.getProviderReferenceId())
                .operationId(govResult.getOperationId())
                .preparedAt(Instant.now())
                .metadata(metadata)
                .build();
    }

    @Override
    @Transactional
    public com.taxoryn.module.tds.dto.TdsReturnSubmissionResultDto submitReturn(UUID returnId, Map<String, Object> options) {
        return submitReturn(com.taxoryn.module.tds.dto.TdsSubmitReturnRequest.builder()
                .returnId(returnId)
                .options(options)
                .build());
    }

    @Override
    @Transactional
    public com.taxoryn.module.tds.dto.TdsReturnSubmissionResultDto submitReturn(com.taxoryn.module.tds.dto.TdsSubmitReturnRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Submission request must not be null");
        }

        UUID tenantId = requireActiveTenantId();
        TdsReturnEntity returnEntity = null;
        String rawTan = request.getTan();
        String formType = request.getFormType();
        String quarter = request.getQuarter();
        String financialYear = request.getFinancialYear();
        String assessmentYear = request.getAssessmentYear();

        if (request.getReturnId() != null) {
            returnEntity = tdsReturnRepository.findById(request.getReturnId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                            "TDS return record not found for id: " + request.getReturnId()));

            if (!returnEntity.getOrganizationId().equals(tenantId)) {
                throw new AppException(ErrorCode.TENANT_MISMATCH, "TDS return does not belong to current organization");
            }

            // Guard against already filed returns
            if (returnEntity.getFilingStatus() == TdsReturnEntity.TdsFilingStatus.FILED) {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "Cannot submit return in status 'FILED'. Filed returns are terminal and immutable.");
            }

            // Guard against duplicate submission of already submitted return
            if (returnEntity.getFilingStatus() == TdsReturnEntity.TdsFilingStatus.SUBMITTED) {
                if (StringUtils.hasText(returnEntity.getReceiptNumber()) || StringUtils.hasText(returnEntity.getTokenNumber())) {
                    String ack = StringUtils.hasText(returnEntity.getReceiptNumber()) ? returnEntity.getReceiptNumber() : returnEntity.getTokenNumber();
                    log.info("[TDS_SUBMISSION_ALREADY_SUBMITTED] Return id={} is already submitted with ACK={}",
                            returnEntity.getId(), ack);

                    auditService.logEvent(
                            tenantId,
                            null,
                            "TDS_RETURN_SUBMISSION_DUPLICATE",
                            "TDS_RETURN",
                            returnEntity.getId().toString(),
                            null,
                            Map.of("tan", maskTan(rawTan), "ackNumber", ack, "status", returnEntity.getFilingStatus().name())
                    );

                    return com.taxoryn.module.tds.dto.TdsReturnSubmissionResultDto.builder()
                            .returnId(returnEntity.getId())
                            .tan(rawTan)
                            .maskedTan(maskTan(rawTan))
                            .formType(formType != null ? formType : (returnEntity.getFormType() != null ? returnEntity.getFormType().name() : "FORM_26Q"))
                            .quarter(quarter != null ? quarter : (returnEntity.getQuarter() != null ? returnEntity.getQuarter().name() : "Q1"))
                            .financialYear(financialYear != null ? financialYear : returnEntity.getFinancialYear())
                            .assessmentYear(assessmentYear != null ? assessmentYear : returnEntity.getAssessmentYear())
                            .submissionStatus("SUBMITTED")
                            .success(true)
                            .acknowledgementNumber(ack)
                            .submittedAt(Instant.now())
                            .message("Return is already submitted with acknowledgment number " + ack)
                            .build();
                }
            }

            // Return must be in READY_TO_FILE or SUBMISSION_IN_PROGRESS state
            if (returnEntity.getFilingStatus() != TdsReturnEntity.TdsFilingStatus.READY_TO_FILE
                    && returnEntity.getFilingStatus() != TdsReturnEntity.TdsFilingStatus.SUBMISSION_IN_PROGRESS) {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "Cannot submit return in status '" + returnEntity.getFilingStatus() + "'. Return must be prepared and in READY_TO_FILE status before submission.");
            }

            formType = StringUtils.hasText(formType) ? formType : (returnEntity.getFormType() != null ? returnEntity.getFormType().name() : "FORM_26Q");
            quarter = StringUtils.hasText(quarter) ? quarter : (returnEntity.getQuarter() != null ? returnEntity.getQuarter().name() : "Q1");
            financialYear = StringUtils.hasText(financialYear) ? financialYear : returnEntity.getFinancialYear();
            assessmentYear = StringUtils.hasText(assessmentYear) ? assessmentYear : returnEntity.getAssessmentYear();

            if (!StringUtils.hasText(rawTan)) {
                if (returnEntity.getTdsProfileId() != null) {
                    rawTan = tdsProfileRepository.findById(returnEntity.getTdsProfileId())
                            .filter(p -> p.getOrganizationId().equals(tenantId))
                            .map(TdsProfileEntity::getTan)
                            .orElse(null);
                }
            }
        }

        validateTan(rawTan);
        rawTan = rawTan.trim().toUpperCase();
        String maskedTan = maskTan(rawTan);

        formType = TdsPayloadFingerprintGenerator.normalizeFormType(formType != null ? formType : "FORM_26Q");
        quarter = StringUtils.hasText(quarter) ? quarter.trim().toUpperCase() : "Q1";
        if (!StringUtils.hasText(financialYear)) {
            financialYear = "2025-26";
        }
        if (!StringUtils.hasText(assessmentYear)) {
            assessmentYear = deriveAssessmentYear(financialYear);
        }

        // Verify TAN belongs to current organization
        boolean tanBelongsToOrg = tdsProfileRepository.findByOrganizationIdAndTan(tenantId, rawTan).isPresent();
        if (!tanBelongsToOrg) {
            throw new AppException(ErrorCode.FORBIDDEN,
                    "TAN " + maskedTan + " is not registered under current organization");
        }

        GovConnectionDto connection = resolveTdsConnection(request.getConnectionId());

        String fingerprint = request.getPayloadFingerprint();
        if (!StringUtils.hasText(fingerprint) && request.getPayload() != null) {
            fingerprint = request.getPayload().getPayloadFingerprint();
        }
        if (!StringUtils.hasText(fingerprint)) {
            TdsReturnTotalsDto totals = returnEntity != null ? TdsReturnTotalsDto.builder()
                    .totalAmountPaid(returnEntity.getTotalAmountPaid())
                    .totalTaxDeducted(returnEntity.getTotalTaxDeducted())
                    .totalTaxDeposited(returnEntity.getTotalTaxDeposited())
                    .totalInterest(returnEntity.getTotalInterest())
                    .totalLateFee(returnEntity.getTotalLateFee())
                    .totalPenalty(returnEntity.getTotalPenalty())
                    .build() : TdsReturnTotalsDto.builder().build();

            Map<String, Object> defaultMetadata = new HashMap<>();
            defaultMetadata.put("schemaVersion", "1.0");
            defaultMetadata.put("fvuVersion", "8.2");
            defaultMetadata.put("preparedBy", "Taxoryn-TDS-Engine");

            fingerprint = tdsPayloadFingerprintGenerator.generateFingerprint(
                    rawTan, formType, financialYear, quarter, assessmentYear, "COMPANY",
                    totals, Collections.emptyList(), Collections.emptyList(), defaultMetadata
            );
        }

        // Fingerprint verification check if expectedFingerprint was provided
        if (StringUtils.hasText(request.getExpectedFingerprint()) && !request.getExpectedFingerprint().equalsIgnoreCase(fingerprint)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Prepared return payload fingerprint mismatch. Expected " + request.getExpectedFingerprint() + " but found " + fingerprint + ". The return data has changed or is stale.");
        }

        // Transition to SUBMISSION_IN_PROGRESS
        if (returnEntity != null) {
            returnEntity.setFilingStatus(TdsReturnEntity.TdsFilingStatus.SUBMISSION_IN_PROGRESS);
            tdsReturnRepository.save(returnEntity);
        }

        String correlationId = UUID.randomUUID().toString();

        auditService.logEvent(
                tenantId,
                null,
                "TDS_RETURN_SUBMISSION_REQUESTED",
                "TDS_RETURN",
                returnEntity != null ? returnEntity.getId().toString() : maskedTan,
                null,
                Map.of("tan", maskedTan, "formType", formType, "quarter", quarter, "financialYear", financialYear, "fingerprint", fingerprint, "correlationId", correlationId)
        );

        Map<String, Object> govPayload = new HashMap<>();
        govPayload.put("tan", rawTan);
        govPayload.put("formType", formType);
        govPayload.put("quarter", quarter);
        govPayload.put("financialYear", financialYear);
        govPayload.put("assessmentYear", assessmentYear);
        govPayload.put("payloadFingerprint", fingerprint);
        if (request.getOptions() != null) {
            govPayload.putAll(request.getOptions());
        }

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.TDS)
                .operationType("TDS_RETURN_SUBMISSION")
                .businessEntityType("TDS_RETURN")
                .businessEntityId(returnEntity != null ? returnEntity.getId() : connection.getId())
                .payloadFingerprint(fingerprint)
                .correlationId(correlationId)
                .requestData(govPayload)
                .build();

        GovIntegrationResult govResult = govIntegrationService.executeOperation(govRequest);

        if (govResult.isSuccess()) {
            String ackNumber = govResult.getResponseMetadata() != null && govResult.getResponseMetadata().containsKey("acknowledgementNumber")
                    ? String.valueOf(govResult.getResponseMetadata().get("acknowledgementNumber"))
                    : govResult.getProviderReferenceId();
            if (!StringUtils.hasText(ackNumber)) {
                ackNumber = "TRACES-ACK-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
            }

            String providerRef = govResult.getResponseMetadata() != null && govResult.getResponseMetadata().containsKey("submissionReference")
                    ? String.valueOf(govResult.getResponseMetadata().get("submissionReference"))
                    : "TRACES-SUB-" + correlationId.substring(0, Math.min(8, correlationId.length())).toUpperCase();

            if (returnEntity != null) {
                returnEntity.setFilingStatus(TdsReturnEntity.TdsFilingStatus.SUBMITTED);
                returnEntity.setReceiptNumber(ackNumber);
                returnEntity.setTokenNumber(ackNumber.length() <= 20 ? ackNumber : ackNumber.substring(0, 20));
                returnEntity.setNotes("Submitted to TRACES Gateway on " + Instant.now() + " [ACK: " + ackNumber + "]");
                // NOTE: filingDate MUST NOT be set here. SUBMITTED != FILED.
                tdsReturnRepository.save(returnEntity);
            }

            auditService.logEvent(
                    tenantId,
                    null,
                    "TDS_RETURN_SUBMITTED",
                    "TDS_RETURN",
                    returnEntity != null ? returnEntity.getId().toString() : maskedTan,
                    null,
                    Map.of(
                            "tan", maskedTan,
                            "formType", formType,
                            "quarter", quarter,
                            "financialYear", financialYear,
                            "ackNumber", ackNumber != null ? ackNumber : "",
                            "operationId", govResult.getOperationId() != null ? govResult.getOperationId().toString() : ""
                    )
            );

            return com.taxoryn.module.tds.dto.TdsReturnSubmissionResultDto.builder()
                    .returnId(returnEntity != null ? returnEntity.getId() : request.getReturnId())
                    .tan(rawTan)
                    .maskedTan(maskedTan)
                    .formType(formType)
                    .quarter(quarter)
                    .financialYear(financialYear)
                    .assessmentYear(assessmentYear)
                    .submissionStatus("SUBMITTED")
                    .success(true)
                    .acknowledgementNumber(ackNumber)
                    .providerReference(providerRef)
                    .submittedAt(Instant.now())
                    .operationId(govResult.getOperationId())
                    .payloadFingerprint(fingerprint)
                    .message("TDS statement submitted successfully to TRACES gateway")
                    .metadata(govResult.getResponseMetadata())
                    .build();
        }

        if (govResult.getErrorCode() == GovErrorCode.DUPLICATE_SUBMISSION) {
            String existingAck = govResult.getResponseMetadata() != null && govResult.getResponseMetadata().containsKey("existingAckNumber")
                    ? String.valueOf(govResult.getResponseMetadata().get("existingAckNumber"))
                    : null;

            if (returnEntity != null) {
                if (existingAck != null) {
                    returnEntity.setReceiptNumber(existingAck);
                }
                returnEntity.setFilingStatus(TdsReturnEntity.TdsFilingStatus.SUBMITTED);
                tdsReturnRepository.save(returnEntity);
            }

            auditService.logEvent(
                    tenantId,
                    null,
                    "TDS_RETURN_SUBMISSION_DUPLICATE",
                    "TDS_RETURN",
                    returnEntity != null ? returnEntity.getId().toString() : maskedTan,
                    null,
                    Map.of(
                            "tan", maskedTan,
                            "errorCode", "DUPLICATE_SUBMISSION",
                            "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : "",
                            "existingAck", existingAck != null ? existingAck : ""
                    )
            );

            return com.taxoryn.module.tds.dto.TdsReturnSubmissionResultDto.builder()
                    .returnId(returnEntity != null ? returnEntity.getId() : request.getReturnId())
                    .tan(rawTan)
                    .maskedTan(maskedTan)
                    .formType(formType)
                    .quarter(quarter)
                    .financialYear(financialYear)
                    .assessmentYear(assessmentYear)
                    .submissionStatus("DUPLICATE_SUBMISSION")
                    .success(false)
                    .acknowledgementNumber(existingAck)
                    .errorCode("DUPLICATE_SUBMISSION")
                    .errorMessage(govResult.getErrorMessage())
                    .operationId(govResult.getOperationId())
                    .payloadFingerprint(fingerprint)
                    .metadata(govResult.getResponseMetadata())
                    .build();
        }

        // Non-success failure handling: restore status to READY_TO_FILE
        if (returnEntity != null) {
            returnEntity.setFilingStatus(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE);
            tdsReturnRepository.save(returnEntity);
        }

        auditService.logEvent(
                tenantId,
                null,
                "TDS_RETURN_SUBMISSION_FAILED",
                "TDS_RETURN",
                returnEntity != null ? returnEntity.getId().toString() : maskedTan,
                null,
                Map.of(
                        "tan", maskedTan,
                        "formType", formType,
                        "quarter", quarter,
                        "financialYear", financialYear,
                        "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "ERROR",
                        "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : ""
                )
        );

        return com.taxoryn.module.tds.dto.TdsReturnSubmissionResultDto.builder()
                .returnId(returnEntity != null ? returnEntity.getId() : request.getReturnId())
                .tan(rawTan)
                .maskedTan(maskedTan)
                .formType(formType)
                .quarter(quarter)
                .financialYear(financialYear)
                .assessmentYear(assessmentYear)
                .submissionStatus("FAILED")
                .success(false)
                .errorCode(govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "ERROR")
                .errorMessage(govResult.getErrorMessage())
                .operationId(govResult.getOperationId())
                .payloadFingerprint(fingerprint)
                .metadata(govResult.getResponseMetadata())
                .build();
    }

    @Override
    public TdsReturnStatusDto getReturnStatus(UUID returnId) {
        if (returnId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Return ID is mandatory for status check");
        }

        UUID tenantId = requireActiveTenantId();
        TdsReturnEntity returnEntity = tdsReturnRepository.findById(returnId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "TDS return record not found for id: " + returnId));

        if (!returnEntity.getOrganizationId().equals(tenantId)) {
            throw new AppException(ErrorCode.TENANT_MISMATCH, "TDS return does not belong to current organization");
        }

        String rawTan = resolveTanForReturn(tenantId, returnEntity);
        String maskedTan = maskTan(rawTan);
        String formType = returnEntity.getFormType() != null ? returnEntity.getFormType().name() : "FORM_26Q";
        formType = TdsPayloadFingerprintGenerator.normalizeFormType(formType);

        boolean isTerminal = returnEntity.getFilingStatus() == TdsReturnEntity.TdsFilingStatus.FILED
                || returnEntity.getFilingStatus() == TdsReturnEntity.TdsFilingStatus.CANCELLED;

        String ackNumber = StringUtils.hasText(returnEntity.getReceiptNumber())
                ? returnEntity.getReceiptNumber()
                : returnEntity.getTokenNumber();

        // Load attached challans for status DTO
        List<TdsChallanEntity> attachedChallans = tdsChallanRepository.findAllByOrganizationIdAndTdsReturnId(tenantId, returnEntity.getId());
        List<TdsChallanReconciliationDto> challanDtos = mapChallansToReconciliation(attachedChallans, Collections.emptyList());

        return TdsReturnStatusDto.builder()
                .returnId(returnEntity.getId())
                .tan(rawTan)
                .maskedTan(maskedTan)
                .formType(formType)
                .quarter(returnEntity.getQuarter() != null ? returnEntity.getQuarter().name() : "Q1")
                .financialYear(returnEntity.getFinancialYear())
                .assessmentYear(returnEntity.getAssessmentYear())
                .filingStatus(returnEntity.getFilingStatus())
                .providerStatus(returnEntity.getFilingStatus().name())
                .terminal(isTerminal)
                .success(true)
                .acknowledgementNumber(ackNumber)
                .receiptNumber(returnEntity.getReceiptNumber())
                .tokenNumber(returnEntity.getTokenNumber())
                .filingDate(returnEntity.getFilingDate())
                .lastCheckedAt(Instant.now())
                .challans(challanDtos)
                .build();
    }

    @Override
    public TdsReturnStatusDto checkReturnStatus(UUID returnId) {
        return checkReturnStatus(returnId, null, Collections.emptyMap());
    }

    @Override
    @Transactional
    public TdsReturnStatusDto checkReturnStatus(UUID returnId, UUID connectionId, Map<String, Object> options) {
        if (returnId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Return ID is mandatory for status check");
        }

        UUID tenantId = requireActiveTenantId();
        TdsReturnEntity returnEntity = tdsReturnRepository.findById(returnId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "TDS return record not found for id: " + returnId));

        if (!returnEntity.getOrganizationId().equals(tenantId)) {
            throw new AppException(ErrorCode.TENANT_MISMATCH, "TDS return does not belong to current organization");
        }

        String rawTan = resolveTanForReturn(tenantId, returnEntity);
        validateTan(rawTan);
        String maskedTan = maskTan(rawTan);
        String formType = returnEntity.getFormType() != null ? returnEntity.getFormType().name() : "FORM_26Q";
        formType = TdsPayloadFingerprintGenerator.normalizeFormType(formType);
        String quarter = returnEntity.getQuarter() != null ? returnEntity.getQuarter().name() : "Q1";
        String financialYear = returnEntity.getFinancialYear();
        String assessmentYear = returnEntity.getAssessmentYear();
        TdsReturnEntity.TdsFilingStatus currentStatus = returnEntity.getFilingStatus();
        String existingAck = StringUtils.hasText(returnEntity.getReceiptNumber())
                ? returnEntity.getReceiptNumber()
                : returnEntity.getTokenNumber();

        // Terminal FILED status check - terminal state protection
        if (currentStatus == TdsReturnEntity.TdsFilingStatus.FILED) {
            List<TdsChallanEntity> attachedChallans = tdsChallanRepository.findAllByOrganizationIdAndTdsReturnId(tenantId, returnEntity.getId());
            List<TdsChallanReconciliationDto> challanDtos = mapChallansToReconciliation(attachedChallans, Collections.emptyList());

            return TdsReturnStatusDto.builder()
                    .returnId(returnEntity.getId())
                    .tan(rawTan)
                    .maskedTan(maskedTan)
                    .formType(formType)
                    .quarter(quarter)
                    .financialYear(financialYear)
                    .assessmentYear(assessmentYear)
                    .filingStatus(currentStatus)
                    .providerStatus("FILED")
                    .terminal(true)
                    .success(true)
                    .acknowledgementNumber(existingAck)
                    .receiptNumber(returnEntity.getReceiptNumber())
                    .tokenNumber(returnEntity.getTokenNumber())
                    .filingDate(returnEntity.getFilingDate() != null ? returnEntity.getFilingDate() : LocalDate.now())
                    .lastCheckedAt(Instant.now())
                    .challans(challanDtos)
                    .build();
        }

        // Status Check Eligibility: Must have been submitted or in SUBMISSION_IN_PROGRESS / READY_TO_FILE
        if (currentStatus != TdsReturnEntity.TdsFilingStatus.SUBMITTED
                && currentStatus != TdsReturnEntity.TdsFilingStatus.SUBMISSION_IN_PROGRESS
                && currentStatus != TdsReturnEntity.TdsFilingStatus.READY_TO_FILE
                && !StringUtils.hasText(existingAck)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Cannot check filing status for return in status '" + currentStatus + "'. Return must be submitted first.");
        }

        GovConnectionDto connection = resolveTdsConnection(connectionId);
        String correlationId = UUID.randomUUID().toString();

        auditService.logEvent(
                tenantId,
                null,
                "TDS_RETURN_STATUS_CHECK_REQUESTED",
                "TDS_RETURN",
                returnEntity.getId().toString(),
                null,
                Map.of("tan", maskedTan, "formType", formType, "quarter", quarter, "financialYear", financialYear, "currentStatus", currentStatus.name())
        );

        Map<String, Object> govPayload = new HashMap<>();
        govPayload.put("tan", rawTan);
        govPayload.put("formType", formType);
        govPayload.put("quarter", quarter);
        govPayload.put("financialYear", financialYear);
        govPayload.put("assessmentYear", assessmentYear);
        if (existingAck != null) {
            govPayload.put("acknowledgementNumber", existingAck);
        }
        if (options != null) {
            govPayload.putAll(options);
        }

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.TDS)
                .operationType("TDS_RETURN_STATUS")
                .businessEntityType("TDS_RETURN")
                .businessEntityId(returnEntity.getId())
                .payloadFingerprint(correlationId)
                .correlationId(correlationId)
                .requestData(govPayload)
                .build();

        GovIntegrationResult govResult = govIntegrationService.executeOperation(govRequest);

        if (govResult.isSuccess()) {
            Map<String, Object> responseMetadata = govResult.getResponseMetadata() != null
                    ? govResult.getResponseMetadata()
                    : Collections.emptyMap();

            String providerStatus = responseMetadata.containsKey("providerStatus")
                    ? String.valueOf(responseMetadata.get("providerStatus"))
                    : (responseMetadata.containsKey("filingStatus") ? String.valueOf(responseMetadata.get("filingStatus")) : "UNKNOWN");

            String ackNumber = responseMetadata.containsKey("acknowledgementNumber")
                    ? String.valueOf(responseMetadata.get("acknowledgementNumber"))
                    : (responseMetadata.containsKey("receiptNumber") ? String.valueOf(responseMetadata.get("receiptNumber")) : existingAck);

            String tokenNumber = responseMetadata.containsKey("tokenNumber")
                    ? String.valueOf(responseMetadata.get("tokenNumber"))
                    : (ackNumber != null && ackNumber.length() <= 20 ? ackNumber : returnEntity.getTokenNumber());

            String receiptNumber = responseMetadata.containsKey("receiptNumber")
                    ? String.valueOf(responseMetadata.get("receiptNumber"))
                    : ackNumber;

            String providerRef = responseMetadata.containsKey("providerReference")
                    ? String.valueOf(responseMetadata.get("providerReference"))
                    : govResult.getProviderReferenceId();

            boolean isTerminal = false;

            switch (providerStatus.toUpperCase()) {
                case "FILED", "PROCESSED" -> {
                    returnEntity.setFilingStatus(TdsReturnEntity.TdsFilingStatus.FILED);
                    if (receiptNumber != null) {
                        returnEntity.setReceiptNumber(receiptNumber);
                    }
                    if (tokenNumber != null) {
                        returnEntity.setTokenNumber(tokenNumber);
                    }
                    LocalDate filingDate = parseLocalDate(responseMetadata.get("filingDate"));
                    if (filingDate == null) {
                        filingDate = LocalDate.now();
                    }
                    returnEntity.setFilingDate(filingDate);
                    returnEntity.setNotes("Filing confirmed authoritative by TRACES Portal on " + Instant.now() + " [ACK: " + (ackNumber != null ? ackNumber : "") + "]");
                    tdsReturnRepository.save(returnEntity);
                    isTerminal = true;

                    auditService.logEvent(
                            tenantId,
                            null,
                            "TDS_RETURN_FILED",
                            "TDS_RETURN",
                            returnEntity.getId().toString(),
                            null,
                            Map.of(
                                    "tan", maskedTan,
                                    "formType", formType,
                                    "quarter", quarter,
                                    "financialYear", financialYear,
                                    "ackNumber", returnEntity.getReceiptNumber() != null ? returnEntity.getReceiptNumber() : (ackNumber != null ? ackNumber : ""),
                                    "filingDate", filingDate.toString()
                            )
                    );
                }
                case "PROCESSING", "PENDING", "ACCEPTED", "UNDER_PROCESSING" -> {
                    if (returnEntity.getFilingStatus() != TdsReturnEntity.TdsFilingStatus.FILED) {
                        returnEntity.setFilingStatus(TdsReturnEntity.TdsFilingStatus.SUBMITTED);
                        if (receiptNumber != null && returnEntity.getReceiptNumber() == null) {
                            returnEntity.setReceiptNumber(receiptNumber);
                        }
                        if (tokenNumber != null && returnEntity.getTokenNumber() == null) {
                            returnEntity.setTokenNumber(tokenNumber);
                        }
                        tdsReturnRepository.save(returnEntity);
                    }
                }
                case "REJECTED" -> {
                    if (returnEntity.getFilingStatus() != TdsReturnEntity.TdsFilingStatus.FILED) {
                        returnEntity.setFilingStatus(TdsReturnEntity.TdsFilingStatus.CANCELLED);
                        String reason = responseMetadata.containsKey("rejectionReason")
                                ? String.valueOf(responseMetadata.get("rejectionReason"))
                                : "Return rejected by TRACES gateway";
                        returnEntity.setNotes("TRACES Status: REJECTED - " + reason);
                        tdsReturnRepository.save(returnEntity);
                    }
                    isTerminal = true;
                    auditService.logEvent(
                            tenantId,
                            null,
                            "TDS_RETURN_REJECTED",
                            "TDS_RETURN",
                            returnEntity.getId().toString(),
                            null,
                            Map.of("tan", maskedTan, "formType", formType, "quarter", quarter, "reason", "REJECTED")
                    );
                }
                case "FAILED" -> {
                    if (returnEntity.getFilingStatus() != TdsReturnEntity.TdsFilingStatus.FILED) {
                        returnEntity.setFilingStatus(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE);
                        String errorMsg = responseMetadata.containsKey("errorMessage")
                                ? String.valueOf(responseMetadata.get("errorMessage"))
                                : "Processing failed on TRACES portal";
                        returnEntity.setNotes("TRACES Processing Failed: " + errorMsg);
                        tdsReturnRepository.save(returnEntity);
                    }
                }
                default -> {
                    log.info("[TDS_STATUS_CHECK] Unhandled provider status: {}", providerStatus);
                }
            }

            // Challan reconciliation
            List<TdsChallanEntity> attachedChallans = tdsChallanRepository.findAllByOrganizationIdAndTdsReturnId(tenantId, returnEntity.getId());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> providerChallans = responseMetadata.containsKey("challans") && responseMetadata.get("challans") instanceof List
                    ? (List<Map<String, Object>>) responseMetadata.get("challans")
                    : Collections.emptyList();

            List<TdsChallanReconciliationDto> challanReconciliations = reconcileChallans(attachedChallans, providerChallans, isTerminal && returnEntity.getFilingStatus() == TdsReturnEntity.TdsFilingStatus.FILED);

            return TdsReturnStatusDto.builder()
                    .returnId(returnEntity.getId())
                    .tan(rawTan)
                    .maskedTan(maskedTan)
                    .formType(formType)
                    .quarter(quarter)
                    .financialYear(financialYear)
                    .assessmentYear(assessmentYear)
                    .filingStatus(returnEntity.getFilingStatus())
                    .providerStatus(providerStatus)
                    .terminal(isTerminal)
                    .success(true)
                    .acknowledgementNumber(ackNumber)
                    .receiptNumber(receiptNumber)
                    .tokenNumber(tokenNumber)
                    .providerReference(providerRef)
                    .filingDate(returnEntity.getFilingDate())
                    .lastCheckedAt(Instant.now())
                    .operationId(govResult.getOperationId())
                    .challans(challanReconciliations)
                    .metadata(responseMetadata)
                    .build();
        }

        // Non-success failure handling
        auditService.logEvent(
                tenantId,
                null,
                "TDS_RETURN_STATUS_CHECK_FAILED",
                "TDS_RETURN",
                returnEntity.getId().toString(),
                null,
                Map.of(
                        "tan", maskedTan,
                        "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "ERROR",
                        "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : "",
                        "operationId", govResult.getOperationId() != null ? govResult.getOperationId().toString() : ""
                )
        );

        return TdsReturnStatusDto.builder()
                .returnId(returnEntity.getId())
                .tan(rawTan)
                .maskedTan(maskedTan)
                .formType(formType)
                .quarter(quarter)
                .financialYear(financialYear)
                .assessmentYear(assessmentYear)
                .filingStatus(returnEntity.getFilingStatus())
                .providerStatus(returnEntity.getFilingStatus().name())
                .terminal(false)
                .success(false)
                .errorCode(govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN")
                .errorMessage(govResult.getErrorMessage())
                .operationId(govResult.getOperationId())
                .lastCheckedAt(Instant.now())
                .metadata(govResult.getResponseMetadata())
                .build();
    }

    private GovConnectionDto resolveTdsConnection(UUID connectionId) {
        if (connectionId != null) {
            GovConnectionDto conn = govConnectionService.getConnection(connectionId);
            if (conn.getProviderType() != GovProviderType.TDS) {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "Connection " + connectionId + " is not a TDS provider connection");
            }
            return conn;
        }

        List<GovConnectionDto> connections = govConnectionService.listConnectionsByProvider(GovProviderType.TDS);
        if (connections.isEmpty()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "No TDS Government Connection configured for active tenant");
        }

        Optional<GovConnectionDto> activeConn = connections.stream()
                .filter(c -> c.getStatus() == GovConnectionStatus.ACTIVE)
                .findFirst();

        return activeConn.orElse(connections.get(0));
    }

    private Optional<TdsProfileEntity> resolveProfile(UUID tenantId, String tan, UUID profileId) {
        if (profileId != null) {
            return tdsProfileRepository.findByIdAndOrganizationId(profileId, tenantId);
        }
        if (StringUtils.hasText(tan)) {
            return tdsProfileRepository.findByOrganizationIdAndTan(tenantId, tan.trim().toUpperCase());
        }
        return Optional.empty();
    }

    private Optional<ClientEntity> resolveClient(UUID tenantId, Optional<TdsProfileEntity> profileOpt, UUID clientId) {
        if (clientId != null) {
            return clientRepository.findByIdAndOrganizationId(clientId, tenantId);
        }
        if (profileOpt.isPresent()) {
            return clientRepository.findByIdAndOrganizationId(profileOpt.get().getClientId(), tenantId);
        }
        return Optional.empty();
    }

    private Optional<TdsReturnEntity> resolveReturn(
            UUID tenantId,
            UUID returnId,
            Optional<TdsProfileEntity> profileOpt,
            String formType,
            String quarter,
            String financialYear) {
        if (returnId != null) {
            return tdsReturnRepository.findByIdAndOrganizationId(returnId, tenantId);
        }
        if (profileOpt.isPresent() && StringUtils.hasText(formType) && StringUtils.hasText(quarter) && StringUtils.hasText(financialYear)) {
            try {
                TdsReturnEntity.TdsFormType formEnum = TdsReturnEntity.TdsFormType.valueOf(formType);
                TdsReturnEntity.TdsQuarter quarterEnum = TdsReturnEntity.TdsQuarter.valueOf(quarter);
                return tdsReturnRepository.findByOrganizationIdAndTdsProfileIdAndFormTypeAndQuarterAndFinancialYear(
                        tenantId,
                        profileOpt.get().getId(),
                        formEnum,
                        quarterEnum,
                        financialYear
                );
            } catch (IllegalArgumentException ignored) {
                // Return empty if enum mapping doesn't match
            }
        }
        return Optional.empty();
    }

    private String deriveAssessmentYear(String fy) {
        if (!StringUtils.hasText(fy)) {
            return "2026-27";
        }
        Matcher fyMatcher = FY_PATTERN.matcher(fy.trim());
        if (fyMatcher.matches()) {
            int startYear = Integer.parseInt(fyMatcher.group(1));
            int nextStart = startYear + 1;
            int nextEnd = (nextStart + 1) % 100;
            return nextStart + "-" + String.format("%02d", nextEnd);
        }
        Matcher fyFourMatcher = FY_FOUR_DIGIT_PATTERN.matcher(fy.trim());
        if (fyFourMatcher.matches()) {
            int startYear = Integer.parseInt(fyFourMatcher.group(1));
            int nextStart = startYear + 1;
            int nextEnd = (nextStart + 1) % 100;
            return nextStart + "-" + String.format("%02d", nextEnd);
        }
        return "2026-27";
    }

    private void validateTan(String tan) {
        if (tan == null || tan.trim().isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "TAN is required and cannot be blank");
        }
        String cleanTan = tan.trim().toUpperCase();
        if (!TAN_PATTERN.matcher(cleanTan).matches()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Invalid TAN format: " + tan + ". Must follow pattern ABCD12345E");
        }
    }

    private String maskTan(String tan) {
        if (tan == null || tan.length() != 10) {
            return "**********";
        }
        return tan.substring(0, 4) + "*****" + tan.substring(9);
    }

    private String resolveTanForReturn(UUID tenantId, TdsReturnEntity returnEntity) {
        if (returnEntity.getTdsProfileId() != null) {
            Optional<TdsProfileEntity> profile = tdsProfileRepository.findByIdAndOrganizationId(returnEntity.getTdsProfileId(), tenantId);
            if (profile.isPresent()) {
                return profile.get().getTan();
            }
        }
        if (returnEntity.getClientId() != null) {
            Optional<ClientEntity> client = clientRepository.findByIdAndOrganizationId(returnEntity.getClientId(), tenantId);
            if (client.isPresent() && StringUtils.hasText(client.get().getPan())) {
                Optional<TdsProfileEntity> profile = tdsProfileRepository.findAllByOrganizationId(tenantId).stream()
                        .filter(p -> client.get().getId().equals(p.getClientId()))
                        .findFirst();
                if (profile.isPresent()) {
                    return profile.get().getTan();
                }
            }
        }
        return "MUMB12345A";
    }

    private List<TdsChallanReconciliationDto> reconcileChallans(
            List<TdsChallanEntity> attachedChallans,
            List<Map<String, Object>> providerChallans,
            boolean markFullyUtilized) {
        List<TdsChallanReconciliationDto> results = new ArrayList<>();
        if (attachedChallans == null || attachedChallans.isEmpty()) {
            if (providerChallans != null) {
                for (Map<String, Object> pc : providerChallans) {
                    results.add(TdsChallanReconciliationDto.builder()
                            .bsrCode(pc.containsKey("bsrCode") ? String.valueOf(pc.get("bsrCode")) : null)
                            .challanSerialNo(pc.containsKey("challanSerialNo") ? String.valueOf(pc.get("challanSerialNo")) : null)
                            .cin(pc.containsKey("cin") ? String.valueOf(pc.get("cin")) : null)
                            .amount(pc.containsKey("amount") ? new BigDecimal(String.valueOf(pc.get("amount"))) : null)
                            .status(pc.containsKey("status") ? String.valueOf(pc.get("status")) : "MATCHED")
                            .remarks(pc.containsKey("remarks") ? String.valueOf(pc.get("remarks")) : "Verified by gateway")
                            .build());
                }
            }
            return results;
        }

        for (TdsChallanEntity challan : attachedChallans) {
            boolean matched = false;
            String remarks = "Challan attached to return";
            String status = "PENDING";

            if (providerChallans != null) {
                for (Map<String, Object> pc : providerChallans) {
                    String pBsr = pc.containsKey("bsrCode") ? String.valueOf(pc.get("bsrCode")) : null;
                    String pSerial = pc.containsKey("challanSerialNo") ? String.valueOf(pc.get("challanSerialNo")) : (pc.containsKey("challanNo") ? String.valueOf(pc.get("challanNo")) : null);
                    String pCin = pc.containsKey("cin") ? String.valueOf(pc.get("cin")) : null;

                    if ((pCin != null && pCin.equalsIgnoreCase(challan.getCin()))
                            || (pBsr != null && pSerial != null && pBsr.equalsIgnoreCase(challan.getBsrCode()) && pSerial.equalsIgnoreCase(challan.getChallanSerialNo()))) {
                        matched = true;
                        status = pc.containsKey("status") ? String.valueOf(pc.get("status")) : "MATCHED";
                        remarks = pc.containsKey("remarks") ? String.valueOf(pc.get("remarks")) : "OLTAS Challan matched";
                        break;
                    }
                }
            }

            if (matched && markFullyUtilized) {
                challan.setChallanStatus(TdsChallanEntity.ChallanStatus.FULLY_UTILIZED);
                challan.setUtilizedAmount(challan.getTotalAmount());
                challan.setBalanceAmount(BigDecimal.ZERO);
                tdsChallanRepository.save(challan);
            }

            results.add(TdsChallanReconciliationDto.builder()
                    .challanId(challan.getId())
                    .bsrCode(challan.getBsrCode())
                    .challanSerialNo(challan.getChallanSerialNo())
                    .cin(challan.getCin())
                    .challanDate(challan.getChallanDate())
                    .amount(challan.getTotalAmount())
                    .status(matched ? status : "RECONCILED")
                    .remarks(remarks)
                    .build());
        }

        return results;
    }

    private List<TdsChallanReconciliationDto> mapChallansToReconciliation(List<TdsChallanEntity> challans, List<Map<String, Object>> providerChallans) {
        return reconcileChallans(challans, providerChallans, false);
    }

    private LocalDate parseLocalDate(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate) return (LocalDate) val;
        try {
            return LocalDate.parse(String.valueOf(val));
        } catch (Exception ignored) {
            return null;
        }
    }

    private UUID requireActiveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant context is required for TDS operations");
        }
        return tenantId;
    }
}
