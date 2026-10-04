package com.taxoryn.module.gst.integration;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovConnectionHealthDto;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.model.GovConnectionStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gov.service.GovernmentHealthService;
import com.taxoryn.module.gov.service.GovernmentIntegrationService;
import com.taxoryn.module.gst.dto.GstPrepareReturnRequest;
import com.taxoryn.module.gst.dto.GstPreparedReturnDto;
import com.taxoryn.module.gst.dto.GstReturnPayloadDto;
import com.taxoryn.module.gst.dto.GstReturnTotalsDto;
import com.taxoryn.module.gst.dto.GstReturnValidationResultDto;
import com.taxoryn.module.gst.dto.GstTaxpayerProfileDto;
import com.taxoryn.module.gst.entity.GstMonthlySummaryEntity;
import com.taxoryn.module.gst.entity.GstProfileEntity;
import com.taxoryn.module.gst.entity.GstRegistrationEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.integration.dto.GstHandshakeResponseDto;
import com.taxoryn.module.gst.integration.dto.GstIntegrationResultDto;
import com.taxoryn.module.gst.repository.GstMonthlySummaryRepository;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstRegistrationRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.gst.service.GstPayloadFingerprintGenerator;
import com.taxoryn.module.gst.service.GstReturnValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Enterprise implementation of GstGovernmentIntegrationService.
 * Connects the GST domain to the Government Integration Framework via DTO application boundaries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GstGovernmentIntegrationServiceImpl implements GstGovernmentIntegrationService {

    private static final Pattern GSTIN_PATTERN =
            Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    private final GovernmentIntegrationService govIntegrationService;
    private final GovernmentConnectionService govConnectionService;
    private final GovernmentHealthService govHealthService;
    private final AuditService auditService;
    private final GstReturnFilingRepository gstReturnFilingRepository;
    private final GstMonthlySummaryRepository gstMonthlySummaryRepository;
    private final GstRegistrationRepository gstRegistrationRepository;
    private final GstProfileRepository gstProfileRepository;
    private final GstReturnValidator gstReturnValidator;
    private final GstPayloadFingerprintGenerator gstPayloadFingerprintGenerator;

    @Override
    public GstHandshakeResponseDto checkGstConnectionHealth(UUID connectionId) {
        return checkGstConnectionHealth(connectionId, Collections.emptyMap());
    }

    @Override
    public GstHandshakeResponseDto checkGstConnectionHealth(UUID connectionId, Map<String, Object> directives) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = govConnectionService.getConnection(connectionId);

        if (connection.getProviderType() != GovProviderType.GST) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Connection " + connectionId + " is not a GST provider connection");
        }

        auditService.logEvent(
                tenantId,
                null,
                "GST_PROVIDER_HANDSHAKE",
                "GST_CONNECTION",
                connection.getId().toString(),
                null,
                Map.of("displayName", connection.getDisplayName(), "providerType", connection.getProviderType().name())
        );

        GovConnectionHealthDto health = govHealthService.checkConnectionHealth(connectionId, directives);

        return GstHandshakeResponseDto.builder()
                .connectionId(health.getConnectionId())
                .displayName(health.getDisplayName())
                .healthStatus(health.getHealthStatus() != null ? health.getHealthStatus().name() : "UNKNOWN")
                .latencyMs(health.getLatencyMs())
                .message(health.getMessage())
                .lastHealthCheckAt(health.getLastHealthCheckAt())
                .build();
    }

    @Override
    public GstIntegrationResultDto verifyGstin(UUID connectionId, String gstin) {
        return verifyGstin(connectionId, gstin, Collections.emptyMap());
    }

    @Override
    public GstIntegrationResultDto verifyGstin(UUID connectionId, String gstin, Map<String, Object> options) {
        validateGstin(gstin);

        Map<String, Object> payload = new HashMap<>();
        payload.put("gstin", gstin.trim().toUpperCase());
        if (options != null) {
            payload.putAll(options);
        }

        return executeGstOperation(connectionId, "VERIFY_GSTIN", payload);
    }

    @Override
    public GstTaxpayerProfileDto lookupTaxpayer(String gstin) {
        return lookupTaxpayer(null, gstin, Collections.emptyMap());
    }

    @Override
    public GstTaxpayerProfileDto lookupTaxpayer(UUID connectionId, String gstin, Map<String, Object> options) {
        UUID tenantId = requireActiveTenantId();
        validateGstin(gstin);
        String cleanGstin = gstin.trim().toUpperCase();

        GovConnectionDto connection = resolveGstConnection(connectionId);

        String correlationId = UUID.randomUUID().toString();

        auditService.logEvent(
                tenantId,
                null,
                "GST_TAXPAYER_LOOKUP_REQUESTED",
                "GST_CONNECTION",
                connection.getId().toString(),
                null,
                Map.of("gstin", cleanGstin, "correlationId", correlationId)
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("gstin", cleanGstin);
        if (options != null) {
            payload.putAll(options);
        }

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.GST)
                .operationType("VERIFY_GSTIN")
                .businessEntityType("GST_CONNECTION")
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
                    "GST_TAXPAYER_LOOKUP_FAILED",
                    "GST_CONNECTION",
                    connection.getId().toString(),
                    null,
                    Map.of(
                            "gstin", cleanGstin,
                            "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN",
                            "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : ""
                    )
            );

            return GstTaxpayerProfileDto.builder()
                    .gstin(cleanGstin)
                    .valid(false)
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
                "GST_TAXPAYER_LOOKUP_COMPLETED",
                "GST_CONNECTION",
                connection.getId().toString(),
                null,
                Map.of("gstin", cleanGstin, "ackNumber", govResult.getProviderReferenceId() != null ? govResult.getProviderReferenceId() : "")
        );

        Map<String, Object> data = govResult.getResponseMetadata() != null ? govResult.getResponseMetadata() : Collections.emptyMap();

        return GstTaxpayerProfileDto.builder()
                .gstin(cleanGstin)
                .legalName((String) data.get("legalName"))
                .tradeName((String) data.get("tradeName"))
                .status((String) data.getOrDefault("status", "ACTIVE"))
                .registrationDate((String) data.get("registrationDate"))
                .registrationType((String) data.get("registrationType"))
                .stateCode((String) data.get("stateCode"))
                .centerJurisdiction((String) data.get("centerJurisdiction"))
                .stateJurisdiction((String) data.get("stateJurisdiction"))
                .constitutionOfBusiness((String) data.get("constitutionOfBusiness"))
                .taxpayerType((String) data.get("taxpayerType"))
                .address((String) data.get("address"))
                .lastUpdatedDate((String) data.get("lastUpdatedDate"))
                .valid(true)
                .providerReferenceId(govResult.getProviderReferenceId())
                .operationId(govResult.getOperationId())
                .verifiedAt(now)
                .rawData(data)
                .build();
    }

    @Override
    @Transactional
    public GstPreparedReturnDto prepareReturn(GstPrepareReturnRequest request) {
        UUID tenantId = requireActiveTenantId();
        if (request == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Return preparation request must not be null");
        }

        String rawGstin = request.getGstin() != null ? request.getGstin().trim().toUpperCase() : "";
        String returnType = request.getReturnType() != null ? request.getReturnType().trim().toUpperCase().replace("-", "") : "";
        String returnPeriod = request.getReturnPeriod() != null ? request.getReturnPeriod().trim() : "";
        String financialYear = StringUtils.hasText(request.getFinancialYear()) ? request.getFinancialYear().trim() : "2026-27";

        String correlationId = UUID.randomUUID().toString();

        auditService.logEvent(
                tenantId,
                null,
                "GST_RETURN_PREPARATION_STARTED",
                "GST_RETURN",
                rawGstin,
                null,
                Map.of("gstin", rawGstin, "returnType", returnType, "returnPeriod", returnPeriod, "correlationId", correlationId)
        );

        Optional<GstRegistrationEntity> registrationOpt = gstRegistrationRepository.findByOrganizationIdAndGstin(tenantId, rawGstin);
        Optional<GstProfileEntity> profileOpt = gstProfileRepository.findByOrganizationIdAndGstin(tenantId, rawGstin);

        String legalName = registrationOpt.map(GstRegistrationEntity::getLegalName)
                .orElseGet(() -> profileOpt.map(GstProfileEntity::getLegalName).orElse("Apex Enterprises Private Limited"));
        String tradeName = registrationOpt.map(GstRegistrationEntity::getTradeName)
                .orElseGet(() -> profileOpt.map(GstProfileEntity::getTradeName).orElse("Apex Solutions"));

        GstReturnTotalsDto totals = resolveTotals(request, profileOpt.orElse(null), rawGstin, returnPeriod);
        Map<String, Object> sections = request.getSections() != null ? new HashMap<>(request.getSections()) : new HashMap<>();

        GstReturnValidationResultDto validationResult = gstReturnValidator.validate(request, totals);

        if (!validationResult.isValid()) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "GST_RETURN_VALIDATION_FAILED",
                    "GST_RETURN",
                    rawGstin,
                    null,
                    Map.of(
                            "gstin", rawGstin,
                            "returnType", returnType,
                            "returnPeriod", returnPeriod,
                            "errors", String.join("; ", validationResult.getErrors())
                    )
            );

            return GstPreparedReturnDto.builder()
                    .filingId(request.getFilingId())
                    .gstin(rawGstin)
                    .legalName(legalName)
                    .tradeName(tradeName)
                    .returnType(returnType)
                    .returnPeriod(returnPeriod)
                    .financialYear(financialYear)
                    .status("VALIDATION_FAILED")
                    .readyForSubmission(false)
                    .validationResult(validationResult)
                    .preparedAt(Instant.now())
                    .errorCode("VALIDATION_FAILED")
                    .errorMessage(String.join("; ", validationResult.getErrors()))
                    .build();
        }

        String fingerprint = gstPayloadFingerprintGenerator.generateFingerprint(
                rawGstin, returnType, returnPeriod, totals, sections
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("stateCode", rawGstin.length() >= 2 ? rawGstin.substring(0, 2) : "27");
        metadata.put("schemaVersion", "1.0");
        metadata.put("preparedBy", "Taxoryn-GST-Engine");

        GstReturnPayloadDto payloadDto = GstReturnPayloadDto.builder()
                .gstin(rawGstin)
                .returnType(returnType)
                .returnPeriod(returnPeriod)
                .financialYear(financialYear)
                .sections(sections)
                .totals(totals)
                .payloadFingerprint(fingerprint)
                .metadata(metadata)
                .build();

        GovConnectionDto connection = resolveGstConnection(request.getConnectionId());

        Map<String, Object> govPayload = new HashMap<>();
        govPayload.put("gstin", rawGstin);
        govPayload.put("returnType", returnType);
        govPayload.put("returnPeriod", returnPeriod);
        govPayload.put("financialYear", financialYear);
        govPayload.put("payloadFingerprint", fingerprint);
        if (request.getOptions() != null) {
            govPayload.putAll(request.getOptions());
        }

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.GST)
                .operationType("GST_RETURN_PREPARATION")
                .businessEntityType("GST_RETURN_FILING")
                .businessEntityId(request.getFilingId() != null ? request.getFilingId() : connection.getId())
                .correlationId(correlationId)
                .requestData(govPayload)
                .build();

        GovIntegrationResult govResult = govIntegrationService.executeOperation(govRequest);

        if (!govResult.isSuccess()) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "GST_RETURN_PREPARATION_FAILED",
                    "GST_RETURN",
                    rawGstin,
                    null,
                    Map.of(
                            "gstin", rawGstin,
                            "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN",
                            "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : ""
                    )
            );

            return GstPreparedReturnDto.builder()
                    .filingId(request.getFilingId())
                    .gstin(rawGstin)
                    .legalName(legalName)
                    .tradeName(tradeName)
                    .returnType(returnType)
                    .returnPeriod(returnPeriod)
                    .financialYear(financialYear)
                    .status("FAILED")
                    .readyForSubmission(false)
                    .validationResult(validationResult)
                    .payload(payloadDto)
                    .operationId(govResult.getOperationId())
                    .preparedAt(Instant.now())
                    .errorCode(govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN")
                    .errorMessage(govResult.getErrorMessage())
                    .build();
        }

        if (request.getFilingId() != null) {
            gstReturnFilingRepository.findByIdAndOrganizationId(request.getFilingId(), tenantId).ifPresent(filing -> {
                filing.setFilingStatus(GstReturnFilingEntity.GstFilingStatus.PREPARED);
                filing.setTotalTaxableValue(totals.getTaxableValue());
                filing.setTotalTaxLiability(totals.getTotalTax());
                filing.setTotalItcClaimed(totals.getTotalItc());
                gstReturnFilingRepository.save(filing);
            });
        }

        auditService.logEvent(
                tenantId,
                null,
                "GST_RETURN_PREPARED",
                "GST_RETURN",
                rawGstin,
                null,
                Map.of("gstin", rawGstin, "returnType", returnType, "returnPeriod", returnPeriod, "fingerprint", fingerprint)
        );

        auditService.logEvent(
                tenantId,
                null,
                "GST_RETURN_READY_FOR_SUBMISSION",
                "GST_RETURN",
                rawGstin,
                null,
                Map.of("gstin", rawGstin, "returnType", returnType, "returnPeriod", returnPeriod)
        );

        return GstPreparedReturnDto.builder()
                .filingId(request.getFilingId())
                .gstin(rawGstin)
                .legalName(legalName)
                .tradeName(tradeName)
                .returnType(returnType)
                .returnPeriod(returnPeriod)
                .financialYear(financialYear)
                .status("PREPARED")
                .readyForSubmission(true)
                .validationResult(validationResult)
                .payload(payloadDto)
                .operationId(govResult.getOperationId())
                .providerReferenceId(govResult.getProviderReferenceId())
                .preparedAt(Instant.now())
                .build();
    }

    @Override
    public GstIntegrationResultDto executeGstOperation(UUID connectionId, String operationType, Map<String, Object> payload) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = govConnectionService.getConnection(connectionId);

        if (connection.getProviderType() != GovProviderType.GST) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Connection " + connectionId + " is not a GST provider connection");
        }

        String correlationId = UUID.randomUUID().toString();

        auditService.logEvent(
                tenantId,
                null,
                "GST_INTEGRATION_REQUESTED",
                "GST_INTEGRATION",
                connection.getId().toString(),
                null,
                Map.of("operationType", operationType, "correlationId", correlationId)
        );

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.GST)
                .operationType(operationType)
                .businessEntityType("GST_CONNECTION")
                .businessEntityId(connection.getId())
                .correlationId(correlationId)
                .requestData(payload != null ? payload : Collections.emptyMap())
                .build();

        GovIntegrationResult govResult = govIntegrationService.executeOperation(govRequest);

        if (!govResult.isSuccess()) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "GST_INTEGRATION_FAILED",
                    "GST_INTEGRATION",
                    connection.getId().toString(),
                    null,
                    Map.of(
                            "operationType", operationType,
                            "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN",
                            "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : ""
                    )
            );
        }

        String gstin = payload != null && payload.containsKey("gstin") ? String.valueOf(payload.get("gstin")) : null;

        return GstIntegrationResultDto.builder()
                .success(govResult.isSuccess())
                .operationId(govResult.getOperationId())
                .connectionId(connection.getId())
                .gstin(gstin)
                .operationType(govResult.getOperationType())
                .correlationId(govResult.getCorrelationId())
                .providerReferenceId(govResult.getProviderReferenceId())
                .errorCode(govResult.getErrorCode() != null ? govResult.getErrorCode().name() : null)
                .errorMessage(govResult.getErrorMessage())
                .data(govResult.getResponseMetadata())
                .build();
    }

    private GstReturnTotalsDto resolveTotals(GstPrepareReturnRequest request, GstProfileEntity profile, String gstin, String period) {
        if (request.getSections() != null && request.getSections().containsKey("taxableValue")) {
            BigDecimal taxableValue = parseDecimal(request.getSections().get("taxableValue"));
            BigDecimal igst = parseDecimal(request.getSections().get("igst"));
            BigDecimal cgst = parseDecimal(request.getSections().get("cgst"));
            BigDecimal sgst = parseDecimal(request.getSections().get("sgst"));
            BigDecimal cess = parseDecimal(request.getSections().get("cess"));
            BigDecimal itc = parseDecimal(request.getSections().get("itc"));
            return GstReturnTotalsDto.of(taxableValue, igst, cgst, sgst, cess, itc);
        }

        UUID tenantId = TenantContext.getTenantId();
        if (request.getFilingId() != null) {
            Optional<GstReturnFilingEntity> filingOpt = gstReturnFilingRepository.findByIdAndOrganizationId(request.getFilingId(), tenantId);
            if (filingOpt.isPresent()) {
                GstReturnFilingEntity filing = filingOpt.get();
                if (filing.getTotalTaxableValue().compareTo(BigDecimal.ZERO) > 0 || filing.getTotalTaxLiability().compareTo(BigDecimal.ZERO) > 0) {
                    return GstReturnTotalsDto.of(
                            filing.getTotalTaxableValue(),
                            filing.getTotalTaxLiability(),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            filing.getTotalItcClaimed()
                    );
                }
            }
        }

        if (profile != null) {
            Optional<GstMonthlySummaryEntity> summaryOpt = gstMonthlySummaryRepository.findByOrganizationIdAndGstProfileIdAndPeriod(
                    tenantId, profile.getId(), period
            );
            if (summaryOpt.isPresent()) {
                GstMonthlySummaryEntity summary = summaryOpt.get();
                return GstReturnTotalsDto.of(
                        summary.getTotalSalesTaxable(),
                        summary.getIgstSales(),
                        summary.getCgstSales(),
                        summary.getSgstSales(),
                        summary.getCessSales(),
                        summary.getItcNetClaimed()
                );
            }
        }

        return GstReturnTotalsDto.builder().build();
    }

    private BigDecimal parseDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal b) return b;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(val).trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private GovConnectionDto resolveGstConnection(UUID connectionId) {
        if (connectionId != null) {
            GovConnectionDto conn = govConnectionService.getConnection(connectionId);
            if (conn.getProviderType() != GovProviderType.GST) {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "Connection " + connectionId + " is not a GST provider connection");
            }
            return conn;
        }

        List<GovConnectionDto> connections = govConnectionService.listConnectionsByProvider(GovProviderType.GST);
        if (connections.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "No GST government connection configured for this organization");
        }

        return connections.stream()
                .filter(c -> c.getStatus() == GovConnectionStatus.ACTIVE)
                .findFirst()
                .orElse(connections.get(0));
    }

    private void validateGstin(String gstin) {
        if (!StringUtils.hasText(gstin)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "GSTIN must not be blank");
        }
        if (!GSTIN_PATTERN.matcher(gstin.trim().toUpperCase()).matches()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Invalid GSTIN format: '" + gstin + "'. Expected standard 15-character alphanumeric format (e.g. 27AAAAA0000A1Z5)");
        }
    }

    private UUID requireActiveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant required for GST government integration");
        }
        return tenantId;
    }
}
