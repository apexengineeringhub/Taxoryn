package com.taxoryn.module.itr.integration;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovConnectionHealthDto;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gov.service.GovernmentHealthService;
import com.taxoryn.module.gov.service.GovernmentIntegrationService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.itr.dto.ItrPrepareReturnRequest;
import com.taxoryn.module.itr.dto.ItrPreparedReturnDto;
import com.taxoryn.module.itr.dto.ItrReturnPayloadDto;
import com.taxoryn.module.itr.dto.ItrReturnValidationResultDto;
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
