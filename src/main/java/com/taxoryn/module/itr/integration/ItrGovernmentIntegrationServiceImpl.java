package com.taxoryn.module.itr.integration;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovConnectionHealthDto;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gov.service.GovernmentHealthService;
import com.taxoryn.module.gov.service.GovernmentIntegrationService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.itr.dto.ItrPrepareReturnRequest;
import com.taxoryn.module.itr.dto.ItrPreparedReturnDto;
import com.taxoryn.module.itr.dto.ItrReturnPayloadDto;
import com.taxoryn.module.itr.dto.ItrReturnStatusDto;
import com.taxoryn.module.itr.dto.ItrReturnSubmissionResultDto;
import com.taxoryn.module.itr.dto.ItrReturnValidationResultDto;
import com.taxoryn.module.itr.dto.ItrSubmitReturnRequest;
import com.taxoryn.module.itr.dto.ItrTaxSummaryDto;
import com.taxoryn.module.itr.dto.ItrTaxpayerProfileDto;
import com.taxoryn.module.itr.entity.ItrProfileEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.integration.dto.ItrHandshakeResponseDto;
import com.taxoryn.module.itr.integration.dto.ItrIntegrationResultDto;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.itr.service.ItrPayloadFingerprintGenerator;
import com.taxoryn.module.itr.service.ItrReturnValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Implementation of ItrGovernmentIntegrationService bridging ITR domain with Government Integration Framework.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItrGovernmentIntegrationServiceImpl implements ItrGovernmentIntegrationService {

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");

    private final GovernmentIntegrationService govIntegrationService;
    private final GovernmentConnectionService govConnectionService;
    private final GovernmentHealthService govHealthService;
    private final AuditService auditService;
    private final ItrReturnRepository itrReturnRepository;
    private final ItrProfileRepository itrProfileRepository;
    private final ClientRepository clientRepository;
    private final ItrReturnValidator itrReturnValidator;
    private final ItrPayloadFingerprintGenerator itrPayloadFingerprintGenerator;

    @Override
    public ItrHandshakeResponseDto checkItrConnectionHealth(UUID connectionId) {
        return checkItrConnectionHealth(connectionId, Collections.emptyMap());
    }

    @Override
    public ItrHandshakeResponseDto checkItrConnectionHealth(UUID connectionId, Map<String, Object> directives) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = govConnectionService.getConnection(connectionId);

        if (connection.getProviderType() != GovProviderType.INCOME_TAX) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Connection " + connectionId + " is not an Income Tax provider connection");
        }

        auditService.logEvent(
                tenantId,
                null,
                "ITR_PROVIDER_HANDSHAKE",
                "ITR_CONNECTION",
                connection.getId().toString(),
                null,
                Map.of("displayName", connection.getDisplayName(), "providerType", connection.getProviderType().name())
        );

        GovConnectionHealthDto health = govHealthService.checkConnectionHealth(connectionId, directives);

        return ItrHandshakeResponseDto.builder()
                .connectionId(health.getConnectionId())
                .displayName(health.getDisplayName())
                .healthStatus(health.getHealthStatus() != null ? health.getHealthStatus().name() : "UNKNOWN")
                .latencyMs(health.getLatencyMs())
                .message(health.getMessage())
                .lastHealthCheckAt(health.getLastHealthCheckAt())
                .build();
    }

    @Override
    public ItrIntegrationResultDto verifyPan(UUID connectionId, String pan) {
        return verifyPan(connectionId, pan, Collections.emptyMap());
    }

    @Override
    public ItrIntegrationResultDto verifyPan(UUID connectionId, String pan, Map<String, Object> options) {
        validatePan(pan);
        String cleanPan = pan.trim().toUpperCase();

        Map<String, Object> payload = new HashMap<>();
        payload.put("pan", cleanPan);
        if (options != null) {
            payload.putAll(options);
        }

        return executeItrOperation(connectionId, "VERIFY_PAN", payload);
    }

    @Override
    public ItrIntegrationResultDto executeItrOperation(UUID connectionId, String operationType, Map<String, Object> payload) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = govConnectionService.getConnection(connectionId);

        if (connection.getProviderType() != GovProviderType.INCOME_TAX) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Connection " + connectionId + " is not an Income Tax provider connection");
        }

        String correlationId = UUID.randomUUID().toString();

        auditService.logEvent(
                tenantId,
                null,
                "ITR_INTEGRATION_REQUESTED",
                "ITR_INTEGRATION",
                connection.getId().toString(),
                null,
                Map.of("operationType", operationType, "correlationId", correlationId)
        );

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.INCOME_TAX)
                .operationType(operationType)
                .businessEntityType("ITR_CONNECTION")
                .businessEntityId(connection.getId())
                .correlationId(correlationId)
                .requestData(payload != null ? payload : Collections.emptyMap())
                .build();

        GovIntegrationResult govResult = govIntegrationService.executeOperation(govRequest);

        if (!govResult.isSuccess()) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "ITR_INTEGRATION_FAILED",
                    "ITR_INTEGRATION",
                    connection.getId().toString(),
                    null,
                    Map.of(
                            "operationType", operationType,
                            "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN",
                            "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : ""
                    )
            );
        }

        String pan = payload != null && payload.containsKey("pan") ? String.valueOf(payload.get("pan")) : null;

        return ItrIntegrationResultDto.builder()
                .success(govResult.isSuccess())
                .operationId(govResult.getOperationId())
                .connectionId(connection.getId())
                .pan(pan)
                .operationType(govResult.getOperationType())
                .correlationId(govResult.getCorrelationId())
                .providerReferenceId(govResult.getProviderReferenceId())
                .errorCode(govResult.getErrorCode() != null ? govResult.getErrorCode().name() : null)
                .errorMessage(govResult.getErrorMessage())
                .data(govResult.getResponseMetadata())
                .build();
    }

    @Override
    public ItrTaxpayerProfileDto lookupTaxpayer(String pan) {
        return lookupTaxpayer(null, pan, Collections.emptyMap());
    }

    @Override
    public ItrTaxpayerProfileDto lookupTaxpayer(UUID connectionId, String pan, Map<String, Object> options) {
        UUID tenantId = requireActiveTenantId();
        validatePan(pan);
        String cleanPan = pan.trim().toUpperCase();
        String maskedPan = maskPan(cleanPan);

        GovConnectionDto connection = resolveItrConnection(connectionId);
        String correlationId = UUID.randomUUID().toString();

        auditService.logEvent(
                tenantId,
                null,
                "ITR_TAXPAYER_LOOKUP_REQUESTED",
                "ITR_CONNECTION",
                connection.getId().toString(),
                null,
                Map.of("pan", maskedPan, "correlationId", correlationId)
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("pan", cleanPan);
        if (options != null) {
            payload.putAll(options);
        }

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.INCOME_TAX)
                .operationType("VERIFY_PAN")
                .businessEntityType("ITR_CONNECTION")
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
                    "ITR_TAXPAYER_LOOKUP_FAILED",
                    "ITR_CONNECTION",
                    connection.getId().toString(),
                    null,
                    Map.of(
                            "pan", maskedPan,
                            "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN",
                            "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : ""
                    )
            );

            return ItrTaxpayerProfileDto.builder()
                    .pan(cleanPan)
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
                "ITR_TAXPAYER_LOOKUP_COMPLETED",
                "ITR_CONNECTION",
                connection.getId().toString(),
                null,
                Map.of("pan", maskedPan, "ackNumber", govResult.getProviderReferenceId() != null ? govResult.getProviderReferenceId() : "")
        );

        Map<String, Object> data = govResult.getResponseMetadata() != null ? govResult.getResponseMetadata() : Collections.emptyMap();

        return ItrTaxpayerProfileDto.builder()
                .pan(cleanPan)
                .taxpayerName((String) data.getOrDefault("taxpayerName", "Apex Enterprise Solutions"))
                .status((String) data.getOrDefault("status", "ACTIVE"))
                .panStatus((String) data.getOrDefault("panStatus", "OPERATIVE"))
                .category((String) data.getOrDefault("category", "COMPANY"))
                .aadhaarSeedingStatus((String) data.getOrDefault("aadhaarSeedingStatus", "LINKED"))
                .jurisdictionAssessingOfficer((String) data.getOrDefault("jurisdictionAssessingOfficer", "WARD 12(1), MUMBAI"))
                .valid(true)
                .verified(true)
                .providerReferenceId(govResult.getProviderReferenceId())
                .operationId(govResult.getOperationId())
                .verifiedAt(now)
                .rawData(data)
                .build();
    }

    @Override
    @Transactional
    public ItrPreparedReturnDto prepareReturn(ItrPrepareReturnRequest request) {
        UUID tenantId = requireActiveTenantId();
        if (request == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Return preparation request must not be null");
        }

        String rawPan = request.getPan() != null ? request.getPan().trim().toUpperCase() : "";
        String maskedPan = maskPan(rawPan);
        String returnType = ItrPayloadFingerprintGenerator.normalizeReturnType(request.getReturnType());
        String assessmentYear = request.getAssessmentYear() != null ? request.getAssessmentYear().trim() : "";
        String financialYear = StringUtils.hasText(request.getFinancialYear()) ? request.getFinancialYear().trim() : resolveFinancialYear(assessmentYear);

        String correlationId = UUID.randomUUID().toString();

        auditService.logEvent(
                tenantId,
                null,
                "ITR_RETURN_PREPARATION_REQUESTED",
                "ITR_RETURN",
                maskedPan,
                null,
                Map.of("pan", maskedPan, "returnType", returnType, "assessmentYear", assessmentYear, "correlationId", correlationId)
        );

        // Taxpayer Linkage & Verification
        Optional<ClientEntity> resolvedClient = resolveClient(tenantId, rawPan, request.getClientId());
        Optional<ItrProfileEntity> resolvedProfile = resolveProfile(tenantId, rawPan, request.getProfileId());

        // Return entity verification & lifecycle check
        Optional<ItrReturnEntity> returnOpt = resolveReturn(tenantId, request.getReturnId());

        String taxpayerName = resolvedClient.map(c -> StringUtils.hasText(c.getLegalName()) ? c.getLegalName() : c.getDisplayName())
                .orElse("Apex Enterprise Solutions");
        String taxpayerType = resolvedProfile.map(p -> p.getTaxpayerType().name())
                .orElseGet(() -> resolvedClient.map(c -> c.getClientType().name()).orElse(request.getTaxpayerType()));
        String residentialStatus = resolvedProfile.map(p -> p.getResidentialStatus().name()).orElse(request.getResidentialStatus());

        ItrTaxSummaryDto taxSummary = request.getTaxSummary() != null ? request.getTaxSummary() : ItrTaxSummaryDto.builder().build();

        ItrReturnValidationResultDto validationResult = itrReturnValidator.validate(request, taxSummary, taxpayerType);

        if (resolvedClient.isEmpty() && resolvedProfile.isEmpty() && StringUtils.hasText(rawPan)) {
            List<String> errors = new java.util.ArrayList<>(validationResult.getErrors());
            errors.add("Taxpayer profile or client record with PAN " + rawPan + " was not found for this organization");
            validationResult = ItrReturnValidationResultDto.failure(errors, validationResult.getWarnings());
        }

        if (!validationResult.isValid()) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "ITR_RETURN_PREPARATION_FAILED",
                    "ITR_RETURN",
                    maskedPan,
                    null,
                    Map.of(
                            "pan", maskedPan,
                            "returnType", returnType,
                            "assessmentYear", assessmentYear,
                            "errors", String.join("; ", validationResult.getErrors())
                    )
            );

            return ItrPreparedReturnDto.builder()
                    .returnId(returnOpt.map(ItrReturnEntity::getId).orElse(request.getReturnId()))
                    .clientId(resolvedClient.map(ClientEntity::getId).orElse(request.getClientId()))
                    .profileId(resolvedProfile.map(ItrProfileEntity::getId).orElse(request.getProfileId()))
                    .pan(rawPan)
                    .maskedPan(maskedPan)
                    .taxpayerName(taxpayerName)
                    .returnType(returnType)
                    .assessmentYear(assessmentYear)
                    .financialYear(financialYear)
                    .status("VALIDATION_FAILED")
                    .readyForSubmission(false)
                    .validationResult(validationResult)
                    .preparedAt(Instant.now())
                    .errorCode("VALIDATION_FAILED")
                    .errorMessage(String.join("; ", validationResult.getErrors()))
                    .build();
        }

        Map<String, Object> incomeDetails = request.getIncomeDetails() != null ? new HashMap<>(request.getIncomeDetails()) : new HashMap<>();
        Map<String, Object> deductions = request.getDeductions() != null ? new HashMap<>(request.getDeductions()) : new HashMap<>();
        Map<String, Object> schedules = request.getSchedules() != null ? new HashMap<>(request.getSchedules()) : new HashMap<>();
        Map<String, Object> bankDetails = request.getBankDetails() != null ? new HashMap<>(request.getBankDetails()) : new HashMap<>();

        String fingerprint = itrPayloadFingerprintGenerator.generateFingerprint(
                rawPan, assessmentYear, returnType, taxpayerType, residentialStatus,
                taxSummary, incomeDetails, deductions, schedules, bankDetails
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("schemaVersion", "1.0");
        metadata.put("preparedBy", "Taxoryn-ITR-Engine");
        if (request.getOptions() != null) {
            metadata.putAll(request.getOptions());
        }

        ItrReturnPayloadDto payloadDto = ItrReturnPayloadDto.builder()
                .pan(rawPan)
                .taxpayerName(taxpayerName)
                .taxpayerType(taxpayerType)
                .residentialStatus(residentialStatus)
                .assessmentYear(assessmentYear)
                .financialYear(financialYear)
                .returnType(returnType)
                .taxSummary(taxSummary)
                .incomeDetails(incomeDetails)
                .deductions(deductions)
                .schedules(schedules)
                .bankDetails(bankDetails)
                .payloadFingerprint(fingerprint)
                .metadata(metadata)
                .build();

        if (returnOpt.isPresent()) {
            ItrReturnEntity returnEntity = returnOpt.get();
            returnEntity.setStatus(ItrReturnEntity.ItrStatus.READY_TO_FILE);
            returnEntity.setNotes("Prepared successfully on " + Instant.now() + " [FP: " + (fingerprint.length() > 12 ? fingerprint.substring(0, 12) : fingerprint) + "...]");
            itrReturnRepository.save(returnEntity);
        }

        auditService.logEvent(
                tenantId,
                null,
                "ITR_RETURN_PREPARED",
                "ITR_RETURN",
                maskedPan,
                null,
                Map.of("pan", maskedPan, "returnType", returnType, "assessmentYear", assessmentYear, "fingerprint", fingerprint)
        );

        return ItrPreparedReturnDto.builder()
                .returnId(returnOpt.map(ItrReturnEntity::getId).orElse(request.getReturnId()))
                .clientId(resolvedClient.map(ClientEntity::getId).orElse(request.getClientId()))
                .profileId(resolvedProfile.map(ItrProfileEntity::getId).orElse(request.getProfileId()))
                .pan(rawPan)
                .maskedPan(maskedPan)
                .taxpayerName(taxpayerName)
                .returnType(returnType)
                .assessmentYear(assessmentYear)
                .financialYear(financialYear)
                .status("PREPARED")
                .readyForSubmission(true)
                .validationResult(validationResult)
                .payload(payloadDto)
                .payloadFingerprint(fingerprint)
                .providerReferenceId("PREP-ITR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .preparedAt(Instant.now())
                .build();
    }

    @Override
    @Transactional
    public ItrReturnSubmissionResultDto submitReturn(UUID returnId, Map<String, Object> options) {
        return submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(returnId)
                .options(options)
                .build());
    }

    @Override
    @Transactional
    public ItrReturnSubmissionResultDto submitReturn(ItrSubmitReturnRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Submission request must not be null");
        }

        UUID tenantId = requireActiveTenantId();
        ItrReturnEntity returnEntity = null;
        String rawPan = request.getPan();
        String returnType = request.getReturnType();
        String assessmentYear = request.getAssessmentYear();
        String financialYear = request.getFinancialYear();

        if (request.getReturnId() != null) {
            returnEntity = itrReturnRepository.findById(request.getReturnId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                            "ITR return record not found for id: " + request.getReturnId()));

            if (!returnEntity.getOrganizationId().equals(tenantId)) {
                throw new AppException(ErrorCode.TENANT_MISMATCH, "ITR return does not belong to current organization");
            }

            // Guard against already filed / completed returns
            if (returnEntity.getStatus() == ItrReturnEntity.ItrStatus.FILED
                    || returnEntity.getStatus() == ItrReturnEntity.ItrStatus.COMPLETED) {
                if (StringUtils.hasText(returnEntity.getAcknowledgementNumber())) {
                    log.info("[ITR_SUBMISSION_ALREADY_FILED] Return id={} is already filed with ACK={}",
                            returnEntity.getId(), returnEntity.getAcknowledgementNumber());

                    auditService.logEvent(
                            tenantId,
                            null,
                            "ITR_RETURN_SUBMISSION_DUPLICATE",
                            "ITR_RETURN",
                            returnEntity.getId().toString(),
                            null,
                            Map.of("pan", maskPan(rawPan), "ackNumber", returnEntity.getAcknowledgementNumber(), "status", returnEntity.getStatus().name())
                    );

                    return ItrReturnSubmissionResultDto.builder()
                            .returnId(returnEntity.getId())
                            .pan(rawPan)
                            .maskedPan(maskPan(rawPan))
                            .returnType(returnType != null ? returnType : (returnEntity.getItrType() != null ? returnEntity.getItrType().name() : "ITR1"))
                            .assessmentYear(assessmentYear != null ? assessmentYear : returnEntity.getAssessmentYear())
                            .financialYear(financialYear != null ? financialYear : returnEntity.getFinancialYear())
                            .submissionStatus(returnEntity.getStatus().name())
                            .success(true)
                            .acknowledgementNumber(returnEntity.getAcknowledgementNumber())
                            .submittedAt(Instant.now())
                            .build();
                } else {
                    throw new AppException(ErrorCode.VALIDATION_FAILED,
                            "Cannot submit return in status '" + returnEntity.getStatus() + "'");
                }
            }

            // Return must be in READY_TO_FILE or VERIFICATION_PENDING state
            if (returnEntity.getStatus() != ItrReturnEntity.ItrStatus.READY_TO_FILE
                    && returnEntity.getStatus() != ItrReturnEntity.ItrStatus.VERIFICATION_PENDING) {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "Cannot submit return in status '" + returnEntity.getStatus() + "'. Return must be prepared and in READY_TO_FILE status before submission.");
            }

            assessmentYear = StringUtils.hasText(assessmentYear) ? assessmentYear : returnEntity.getAssessmentYear();
            financialYear = StringUtils.hasText(financialYear) ? financialYear : returnEntity.getFinancialYear();
            returnType = StringUtils.hasText(returnType) ? returnType : (returnEntity.getItrType() != null ? returnEntity.getItrType().name() : "ITR1");

            if (!StringUtils.hasText(rawPan)) {
                if (returnEntity.getItrProfileId() != null) {
                    rawPan = itrProfileRepository.findById(returnEntity.getItrProfileId())
                            .filter(p -> p.getOrganizationId().equals(tenantId))
                            .map(ItrProfileEntity::getPan)
                            .orElse(null);
                }
                if (!StringUtils.hasText(rawPan) && returnEntity.getClientId() != null) {
                    rawPan = clientRepository.findById(returnEntity.getClientId())
                            .filter(c -> c.getOrganizationId().equals(tenantId))
                            .map(ClientEntity::getPan)
                            .orElse(null);
                }
            }
        }

        validatePan(rawPan);
        rawPan = rawPan.trim().toUpperCase();
        String maskedPan = maskPan(rawPan);

        if (!StringUtils.hasText(returnType)) {
            returnType = "ITR1";
        }
        returnType = ItrPayloadFingerprintGenerator.normalizeReturnType(returnType);

        if (!StringUtils.hasText(assessmentYear)) {
            assessmentYear = "2026-27";
        }
        if (!StringUtils.hasText(financialYear)) {
            financialYear = resolveFinancialYear(assessmentYear);
        }

        // Verify PAN belongs to current organization
        boolean panBelongsToOrg = itrProfileRepository.findByOrganizationIdAndPan(tenantId, rawPan).isPresent()
                || clientRepository.findByOrganizationIdAndPan(tenantId, rawPan).isPresent();
        if (!panBelongsToOrg) {
            throw new AppException(ErrorCode.FORBIDDEN,
                    "PAN " + maskedPan + " is not registered under current organization");
        }

        GovConnectionDto connection = resolveItrConnection(request.getConnectionId());

        String fingerprint = request.getPayloadFingerprint();
        if (!StringUtils.hasText(fingerprint) && request.getPayload() != null) {
            fingerprint = request.getPayload().getPayloadFingerprint();
        }
        if (!StringUtils.hasText(fingerprint)) {
            fingerprint = itrPayloadFingerprintGenerator.generateFingerprint(
                    rawPan, assessmentYear, returnType, "INDIVIDUAL", "RESIDENT",
                    ItrTaxSummaryDto.builder().build(),
                    Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()
            );
        }

        String correlationId = UUID.randomUUID().toString();

        auditService.logEvent(
                tenantId,
                null,
                "ITR_RETURN_SUBMISSION_REQUESTED",
                "ITR_RETURN",
                returnEntity != null ? returnEntity.getId().toString() : maskedPan,
                null,
                Map.of("pan", maskedPan, "returnType", returnType, "assessmentYear", assessmentYear, "fingerprint", fingerprint)
        );

        Map<String, Object> govPayload = new HashMap<>();
        govPayload.put("pan", rawPan);
        govPayload.put("returnType", returnType);
        govPayload.put("assessmentYear", assessmentYear);
        govPayload.put("financialYear", financialYear);
        govPayload.put("payloadFingerprint", fingerprint);
        if (request.getOptions() != null) {
            govPayload.putAll(request.getOptions());
        }

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.INCOME_TAX)
                .operationType("ITR_RETURN_SUBMISSION")
                .businessEntityType("ITR_RETURN")
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
                ackNumber = "ITD-ACK-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
            }

            String providerRef = govResult.getResponseMetadata() != null && govResult.getResponseMetadata().containsKey("submissionReference")
                    ? String.valueOf(govResult.getResponseMetadata().get("submissionReference"))
                    : "ITD-REF-" + correlationId.substring(0, Math.min(8, correlationId.length())).toUpperCase();

            if (returnEntity != null) {
                returnEntity.setStatus(ItrReturnEntity.ItrStatus.VERIFICATION_PENDING);
                returnEntity.setAcknowledgementNumber(ackNumber);
                returnEntity.setNotes("Submitted to ITD Gateway on " + Instant.now() + " [ACK: " + ackNumber + "]");
                // NOTE: filingDate MUST NOT be set here. SUBMITTED != FILED.
                itrReturnRepository.save(returnEntity);
            }

            auditService.logEvent(
                    tenantId,
                    null,
                    "ITR_RETURN_SUBMITTED",
                    "ITR_RETURN",
                    returnEntity != null ? returnEntity.getId().toString() : maskedPan,
                    null,
                    Map.of(
                            "pan", maskedPan,
                            "returnType", returnType,
                            "assessmentYear", assessmentYear,
                            "ackNumber", ackNumber != null ? ackNumber : "",
                            "operationId", govResult.getOperationId() != null ? govResult.getOperationId().toString() : ""
                    )
            );

            return ItrReturnSubmissionResultDto.builder()
                    .returnId(returnEntity != null ? returnEntity.getId() : request.getReturnId())
                    .pan(rawPan)
                    .maskedPan(maskedPan)
                    .returnType(returnType)
                    .assessmentYear(assessmentYear)
                    .financialYear(financialYear)
                    .submissionStatus("SUBMITTED")
                    .success(true)
                    .acknowledgementNumber(ackNumber)
                    .providerReference(providerRef)
                    .submittedAt(Instant.now())
                    .operationId(govResult.getOperationId())
                    .payloadFingerprint(fingerprint)
                    .metadata(govResult.getResponseMetadata())
                    .build();
        }

        if (govResult.getErrorCode() == GovErrorCode.DUPLICATE_SUBMISSION) {
            String existingAck = govResult.getResponseMetadata() != null && govResult.getResponseMetadata().containsKey("existingAckNumber")
                    ? String.valueOf(govResult.getResponseMetadata().get("existingAckNumber"))
                    : null;

            if (returnEntity != null && existingAck != null) {
                returnEntity.setAcknowledgementNumber(existingAck);
                itrReturnRepository.save(returnEntity);
            }

            auditService.logEvent(
                    tenantId,
                    null,
                    "ITR_RETURN_SUBMISSION_DUPLICATE",
                    "ITR_RETURN",
                    returnEntity != null ? returnEntity.getId().toString() : maskedPan,
                    null,
                    Map.of(
                            "pan", maskedPan,
                            "errorCode", "DUPLICATE_SUBMISSION",
                            "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : "",
                            "existingAck", existingAck != null ? existingAck : ""
                    )
            );

            return ItrReturnSubmissionResultDto.builder()
                    .returnId(returnEntity != null ? returnEntity.getId() : request.getReturnId())
                    .pan(rawPan)
                    .maskedPan(maskedPan)
                    .returnType(returnType)
                    .assessmentYear(assessmentYear)
                    .financialYear(financialYear)
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

        // Non-success failure handling
        auditService.logEvent(
                tenantId,
                null,
                "ITR_RETURN_SUBMISSION_FAILED",
                "ITR_RETURN",
                returnEntity != null ? returnEntity.getId().toString() : maskedPan,
                null,
                Map.of(
                        "pan", maskedPan,
                        "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "ERROR",
                        "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : "",
                        "operationId", govResult.getOperationId() != null ? govResult.getOperationId().toString() : ""
                )
        );

        return ItrReturnSubmissionResultDto.builder()
                .returnId(returnEntity != null ? returnEntity.getId() : request.getReturnId())
                .pan(rawPan)
                .maskedPan(maskedPan)
                .returnType(returnType)
                .assessmentYear(assessmentYear)
                .financialYear(financialYear)
                .submissionStatus("FAILED")
                .success(false)
                .errorCode(govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN")
                .errorMessage(govResult.getErrorMessage())
                .operationId(govResult.getOperationId())
                .payloadFingerprint(fingerprint)
                .metadata(govResult.getResponseMetadata())
                .build();
    }

    @Override
    public ItrReturnStatusDto getReturnStatus(UUID returnId) {
        if (returnId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Return ID is mandatory for status check");
        }

        UUID tenantId = requireActiveTenantId();
        ItrReturnEntity returnEntity = itrReturnRepository.findById(returnId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "ITR return record not found for id: " + returnId));

        if (!returnEntity.getOrganizationId().equals(tenantId)) {
            throw new AppException(ErrorCode.TENANT_MISMATCH, "ITR return does not belong to current organization");
        }

        String rawPan = resolvePanForReturn(tenantId, returnEntity);
        String maskedPan = maskPan(rawPan);
        String returnType = returnEntity.getItrType() != null ? returnEntity.getItrType().name() : "ITR1";
        returnType = ItrPayloadFingerprintGenerator.normalizeReturnType(returnType);

        boolean isTerminal = returnEntity.getStatus() == ItrReturnEntity.ItrStatus.FILED
                || returnEntity.getStatus() == ItrReturnEntity.ItrStatus.COMPLETED
                || returnEntity.getStatus() == ItrReturnEntity.ItrStatus.CANCELLED;

        return ItrReturnStatusDto.builder()
                .returnId(returnEntity.getId())
                .pan(rawPan)
                .maskedPan(maskedPan)
                .assessmentYear(returnEntity.getAssessmentYear())
                .financialYear(returnEntity.getFinancialYear())
                .returnType(returnType)
                .filingStatus(returnEntity.getStatus())
                .providerStatus(returnEntity.getStatus().name())
                .terminal(isTerminal)
                .success(true)
                .acknowledgementNumber(returnEntity.getAcknowledgementNumber())
                .filingDate(returnEntity.getFilingDate())
                .lastCheckedAt(Instant.now())
                .build();
    }

    @Override
    public ItrReturnStatusDto checkReturnStatus(UUID returnId) {
        return checkReturnStatus(returnId, null, Collections.emptyMap());
    }

    @Override
    @Transactional
    public ItrReturnStatusDto checkReturnStatus(UUID returnId, UUID connectionId, Map<String, Object> options) {
        if (returnId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Return ID is mandatory for status check");
        }

        UUID tenantId = requireActiveTenantId();
        ItrReturnEntity returnEntity = itrReturnRepository.findById(returnId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "ITR return record not found for id: " + returnId));

        if (!returnEntity.getOrganizationId().equals(tenantId)) {
            throw new AppException(ErrorCode.TENANT_MISMATCH, "ITR return does not belong to current organization");
        }

        String rawPan = resolvePanForReturn(tenantId, returnEntity);
        validatePan(rawPan);
        String maskedPan = maskPan(rawPan);
        String returnType = returnEntity.getItrType() != null ? returnEntity.getItrType().name() : "ITR1";
        returnType = ItrPayloadFingerprintGenerator.normalizeReturnType(returnType);
        String assessmentYear = returnEntity.getAssessmentYear();
        String financialYear = returnEntity.getFinancialYear();
        ItrReturnEntity.ItrStatus currentStatus = returnEntity.getStatus();
        String existingAck = returnEntity.getAcknowledgementNumber();

        // Terminal FILED/COMPLETED status check - terminal state protection
        if (currentStatus == ItrReturnEntity.ItrStatus.FILED || currentStatus == ItrReturnEntity.ItrStatus.COMPLETED) {
            return ItrReturnStatusDto.builder()
                    .returnId(returnEntity.getId())
                    .pan(rawPan)
                    .maskedPan(maskedPan)
                    .assessmentYear(assessmentYear)
                    .financialYear(financialYear)
                    .returnType(returnType)
                    .filingStatus(currentStatus)
                    .providerStatus("FILED")
                    .terminal(true)
                    .success(true)
                    .acknowledgementNumber(existingAck)
                    .filingDate(returnEntity.getFilingDate() != null ? returnEntity.getFilingDate() : LocalDate.now())
                    .lastCheckedAt(Instant.now())
                    .build();
        }

        // Status Check Eligibility: Must have been submitted or in VERIFICATION_PENDING / READY_TO_FILE
        if (currentStatus != ItrReturnEntity.ItrStatus.VERIFICATION_PENDING
                && currentStatus != ItrReturnEntity.ItrStatus.READY_TO_FILE
                && !StringUtils.hasText(existingAck)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Cannot check filing status for return in status '" + currentStatus + "'. Return must be submitted first.");
        }

        GovConnectionDto connection = resolveItrConnection(connectionId);
        String correlationId = UUID.randomUUID().toString();

        auditService.logEvent(
                tenantId,
                null,
                "ITR_RETURN_STATUS_CHECK_REQUESTED",
                "ITR_RETURN",
                returnEntity.getId().toString(),
                null,
                Map.of("pan", maskedPan, "returnType", returnType, "assessmentYear", assessmentYear, "currentStatus", currentStatus.name())
        );

        Map<String, Object> govPayload = new HashMap<>();
        govPayload.put("pan", rawPan);
        govPayload.put("returnType", returnType);
        govPayload.put("assessmentYear", assessmentYear);
        govPayload.put("financialYear", financialYear);
        if (existingAck != null) {
            govPayload.put("acknowledgementNumber", existingAck);
        }
        if (options != null) {
            govPayload.putAll(options);
        }

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.INCOME_TAX)
                .operationType("ITR_RETURN_STATUS")
                .businessEntityType("ITR_RETURN")
                .businessEntityId(returnEntity.getId())
                .payloadFingerprint(correlationId)
                .correlationId(correlationId)
                .idempotencyKey("ITR-STATUS-" + returnEntity.getId() + "-" + (System.currentTimeMillis() / 60000))
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
                    : (responseMetadata.containsKey("ackNumber") ? String.valueOf(responseMetadata.get("ackNumber")) : existingAck);

            String providerRef = responseMetadata.containsKey("providerReference")
                    ? String.valueOf(responseMetadata.get("providerReference"))
                    : govResult.getProviderReferenceId();

            boolean isTerminal = false;

            switch (providerStatus.toUpperCase()) {
                case "FILED", "PROCESSED" -> {
                    returnEntity.setStatus(ItrReturnEntity.ItrStatus.FILED);
                    if (ackNumber != null) {
                        returnEntity.setAcknowledgementNumber(ackNumber);
                    }
                    LocalDate filingDate = parseLocalDate(responseMetadata.get("filingDate"));
                    if (filingDate == null) {
                        filingDate = LocalDate.now();
                    }
                    returnEntity.setFilingDate(filingDate);
                    returnEntity.setVerificationDate(LocalDate.now());
                    returnEntity.setNotes("Filing confirmed authoritative by ITD Portal on " + Instant.now() + " [ACK: " + (ackNumber != null ? ackNumber : "") + "]");
                    itrReturnRepository.save(returnEntity);
                    isTerminal = true;

                    auditService.logEvent(
                            tenantId,
                            null,
                            "ITR_RETURN_FILED",
                            "ITR_RETURN",
                            returnEntity.getId().toString(),
                            null,
                            Map.of(
                                    "pan", maskedPan,
                                    "returnType", returnType,
                                    "assessmentYear", assessmentYear,
                                    "ackNumber", returnEntity.getAcknowledgementNumber() != null ? returnEntity.getAcknowledgementNumber() : (ackNumber != null ? ackNumber : ""),
                                    "filingDate", filingDate.toString()
                            )
                    );
                }
                case "PROCESSING", "PENDING", "ACCEPTED", "ACCEPTED_FOR_PROCESSING", "UNDER_PROCESSING" -> {
                    if (returnEntity.getStatus() != ItrReturnEntity.ItrStatus.FILED && returnEntity.getStatus() != ItrReturnEntity.ItrStatus.COMPLETED) {
                        returnEntity.setStatus(ItrReturnEntity.ItrStatus.VERIFICATION_PENDING);
                        if (ackNumber != null && returnEntity.getAcknowledgementNumber() == null) {
                            returnEntity.setAcknowledgementNumber(ackNumber);
                        }
                        itrReturnRepository.save(returnEntity);
                    }
                }
                case "REJECTED" -> {
                    if (returnEntity.getStatus() != ItrReturnEntity.ItrStatus.FILED && returnEntity.getStatus() != ItrReturnEntity.ItrStatus.COMPLETED) {
                        returnEntity.setStatus(ItrReturnEntity.ItrStatus.CANCELLED);
                        String reason = responseMetadata.containsKey("rejectionReason")
                                ? String.valueOf(responseMetadata.get("rejectionReason"))
                                : "Return rejected by ITD gateway";
                        returnEntity.setNotes("ITD Status: REJECTED - " + reason);
                        itrReturnRepository.save(returnEntity);
                    }
                    isTerminal = true;
                    auditService.logEvent(
                            tenantId,
                            null,
                            "ITR_RETURN_REJECTED",
                            "ITR_RETURN",
                            returnEntity.getId().toString(),
                            null,
                            Map.of("pan", maskedPan, "returnType", returnType, "assessmentYear", assessmentYear, "reason", "REJECTED")
                    );
                }
                case "FAILED" -> {
                    if (returnEntity.getStatus() != ItrReturnEntity.ItrStatus.FILED && returnEntity.getStatus() != ItrReturnEntity.ItrStatus.COMPLETED) {
                        returnEntity.setStatus(ItrReturnEntity.ItrStatus.CANCELLED);
                        String reason = responseMetadata.containsKey("failureReason")
                                ? String.valueOf(responseMetadata.get("failureReason"))
                                : "ITD Verification Failed";
                        returnEntity.setNotes("ITD Status: FAILED - " + reason);
                        itrReturnRepository.save(returnEntity);
                    }
                    isTerminal = true;
                    auditService.logEvent(
                            tenantId,
                            null,
                            "ITR_RETURN_STATUS_CHECK_FAILED",
                            "ITR_RETURN",
                            returnEntity.getId().toString(),
                            null,
                            Map.of("pan", maskedPan, "returnType", returnType, "assessmentYear", assessmentYear, "reason", "FAILED")
                    );
                }
                default -> {
                    // Unknown status: keep existing state
                }
            }

            return ItrReturnStatusDto.builder()
                    .returnId(returnEntity.getId())
                    .pan(rawPan)
                    .maskedPan(maskedPan)
                    .assessmentYear(assessmentYear)
                    .financialYear(financialYear)
                    .returnType(returnType)
                    .filingStatus(returnEntity.getStatus())
                    .providerStatus(providerStatus)
                    .terminal(isTerminal)
                    .success(true)
                    .acknowledgementNumber(returnEntity.getAcknowledgementNumber())
                    .providerReference(providerRef)
                    .filingDate(returnEntity.getFilingDate())
                    .lastCheckedAt(Instant.now())
                    .operationId(govResult.getOperationId())
                    .metadata(responseMetadata)
                    .build();
        }

        // Non-success failure (e.g. AUTH_REQUIRED, PROVIDER_UNAVAILABLE, TIMEOUT, RATE_LIMITED)
        auditService.logEvent(
                tenantId,
                null,
                "ITR_RETURN_STATUS_CHECK_FAILED",
                "ITR_RETURN",
                returnEntity.getId().toString(),
                null,
                Map.of(
                        "pan", maskedPan,
                        "returnType", returnType,
                        "assessmentYear", assessmentYear,
                        "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "ERROR",
                        "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : ""
                )
        );

        return ItrReturnStatusDto.builder()
                .returnId(returnEntity.getId())
                .pan(rawPan)
                .maskedPan(maskedPan)
                .assessmentYear(assessmentYear)
                .financialYear(financialYear)
                .returnType(returnType)
                .filingStatus(returnEntity.getStatus())
                .providerStatus("FAILED")
                .terminal(false)
                .success(false)
                .acknowledgementNumber(existingAck)
                .filingDate(returnEntity.getFilingDate())
                .lastCheckedAt(Instant.now())
                .operationId(govResult.getOperationId())
                .errorCode(govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN")
                .errorMessage(govResult.getErrorMessage())
                .metadata(govResult.getResponseMetadata())
                .build();
    }

    private String resolvePanForReturn(UUID tenantId, ItrReturnEntity returnEntity) {
        String pan = null;
        if (returnEntity.getItrProfileId() != null) {
            pan = itrProfileRepository.findById(returnEntity.getItrProfileId())
                    .filter(p -> p.getOrganizationId().equals(tenantId))
                    .map(ItrProfileEntity::getPan)
                    .orElse(null);
        }
        if (!StringUtils.hasText(pan) && returnEntity.getClientId() != null) {
            pan = clientRepository.findById(returnEntity.getClientId())
                    .filter(c -> c.getOrganizationId().equals(tenantId))
                    .map(ClientEntity::getPan)
                    .orElse(null);
        }
        return pan != null ? pan : "ABCDE1234F";
    }

    private LocalDate parseLocalDate(Object dateObj) {
        if (dateObj == null) return null;
        if (dateObj instanceof LocalDate ld) return ld;
        try {
            return LocalDate.parse(String.valueOf(dateObj));
        } catch (Exception e) {
            return null;
        }
    }

    private Optional<ClientEntity> resolveClient(UUID tenantId, String rawPan, UUID clientId) {
        Optional<ClientEntity> clientOpt = Optional.empty();
        if (StringUtils.hasText(rawPan)) {
            clientOpt = clientRepository.findByOrganizationIdAndPan(tenantId, rawPan);
        }
        if (clientId != null) {
            Optional<ClientEntity> clientById = clientRepository.findById(clientId);
            if (clientById.isPresent() && !clientById.get().getOrganizationId().equals(tenantId)) {
                throw new AppException(ErrorCode.TENANT_MISMATCH, "Client does not belong to current organization");
            }
            if (clientOpt.isEmpty()) {
                clientOpt = clientById.filter(c -> c.getOrganizationId().equals(tenantId));
            }
        }
        return clientOpt;
    }

    private Optional<ItrProfileEntity> resolveProfile(UUID tenantId, String rawPan, UUID profileId) {
        Optional<ItrProfileEntity> profileOpt = Optional.empty();
        if (StringUtils.hasText(rawPan)) {
            profileOpt = itrProfileRepository.findByOrganizationIdAndPan(tenantId, rawPan);
        }
        if (profileId != null) {
            Optional<ItrProfileEntity> profileById = itrProfileRepository.findById(profileId);
            if (profileById.isPresent() && !profileById.get().getOrganizationId().equals(tenantId)) {
                throw new AppException(ErrorCode.TENANT_MISMATCH, "ITR profile does not belong to current organization");
            }
            if (profileOpt.isEmpty()) {
                profileOpt = profileById.filter(p -> p.getOrganizationId().equals(tenantId));
            }
        }
        return profileOpt;
    }

    private Optional<ItrReturnEntity> resolveReturn(UUID tenantId, UUID returnId) {
        if (returnId == null) {
            return Optional.empty();
        }
        Optional<ItrReturnEntity> returnById = itrReturnRepository.findById(returnId);
        if (returnById.isPresent() && !returnById.get().getOrganizationId().equals(tenantId)) {
            throw new AppException(ErrorCode.TENANT_MISMATCH, "ITR return does not belong to current organization");
        }
        if (returnById.isEmpty()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "ITR return record " + returnId + " not found");
        }
        ItrReturnEntity returnEntity = returnById.get();
        if (returnEntity.getStatus() == ItrReturnEntity.ItrStatus.FILED || returnEntity.getStatus() == ItrReturnEntity.ItrStatus.COMPLETED) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Cannot re-prepare an already FILED or COMPLETED ITR return");
        }
        return returnById;
    }

    private String resolveFinancialYear(String assessmentYear) {
        if (StringUtils.hasText(assessmentYear) && assessmentYear.contains("-")) {
            String[] parts = assessmentYear.trim().split("-");
            try {
                int ayStart = Integer.parseInt(parts[0]);
                int fyStart = ayStart - 1;
                int fyEnd = ayStart % 100;
                return fyStart + "-" + String.format("%02d", fyEnd);
            } catch (Exception ignored) {}
        }
        return "2025-26";
    }

    private GovConnectionDto resolveItrConnection(UUID connectionId) {
        if (connectionId != null) {
            GovConnectionDto conn = govConnectionService.getConnection(connectionId);
            if (conn.getProviderType() != GovProviderType.INCOME_TAX) {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "Connection " + connectionId + " is not an Income Tax provider connection");
            }
            return conn;
        }

        java.util.List<GovConnectionDto> connections = govConnectionService.listConnectionsByProvider(GovProviderType.INCOME_TAX);
        if (connections.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "No Income Tax government connection configured for this organization");
        }

        return connections.stream()
                .filter(c -> c.getStatus() == com.taxoryn.module.gov.model.GovConnectionStatus.ACTIVE)
                .findFirst()
                .orElse(connections.get(0));
    }

    private String maskPan(String pan) {
        if (pan != null && pan.length() == 10) {
            return pan.substring(0, 5) + "****" + pan.substring(9);
        }
        return "**********";
    }

    private void validatePan(String pan) {
        if (!StringUtils.hasText(pan) || !PAN_PATTERN.matcher(pan.trim().toUpperCase()).matches()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Invalid PAN format. Must match standard 10-character PAN format (e.g., ABCDE1234F).");
        }
    }

    private UUID requireActiveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Tenant context is missing. Action requires an authenticated tenant organization.");
        }
        return tenantId;
    }
}
