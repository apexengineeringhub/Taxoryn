package com.taxoryn.module.compliance.applicability.provider;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.applicability.model.ComplianceFactContext;
import com.taxoryn.module.compliance.profile.entity.ComplianceProfileEntity;
import com.taxoryn.module.compliance.profile.repository.ComplianceProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComplianceFactProviderImpl implements ComplianceFactProvider {

    private final ClientRepository clientRepository;
    private final ComplianceProfileRepository complianceProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public ComplianceFactContext resolveFacts(UUID clientId) {
        UUID orgId = TenantContext.getTenantId();
        if (orgId == null) {
            throw new BadRequestException("Organization tenant context is required to resolve compliance facts.");
        }
        return resolveFacts(orgId, clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceFactContext resolveFacts(UUID organizationId, UUID clientId) {
        if (clientId == null) {
            throw new BadRequestException("Client ID must not be null.");
        }
        if (organizationId == null) {
            throw new BadRequestException("Organization ID must not be null.");
        }

        // 1. Resolve Client
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: '" + clientId + "'"));

        // 2. Resolve Compliance Profile
        Optional<ComplianceProfileEntity> profileOpt = complianceProfileRepository.findByOrganizationIdAndClientId(organizationId, clientId);

        String clientTypeStr = client.getClientType() != null ? client.getClientType().name() : null;

        if (profileOpt.isEmpty()) {
            log.debug("No compliance profile found for client '{}' in org '{}'. Creating unconfigured fact context.", clientId, organizationId);
            return ComplianceFactContext.builder()
                    .clientId(clientId)
                    .organizationId(organizationId)
                    .clientType(clientTypeStr)
                    .profilePresent(false)
                    .build();
        }

        ComplianceProfileEntity profile = profileOpt.get();

        return ComplianceFactContext.builder()
                .clientId(clientId)
                .organizationId(organizationId)
                .clientType(clientTypeStr)
                .profilePresent(true)
                // GST Facts
                .gstApplicable(profile.isGstApplicable())
                .gstRegistrationType(profile.getGstRegistrationType() != null ? profile.getGstRegistrationType().name() : null)
                .gstFilingFrequency(profile.getGstFilingFrequency() != null ? profile.getGstFilingFrequency().name() : null)
                .gstCompositionScheme(profile.isGstCompositionScheme())
                .gstEinvoiceApplicable(profile.isGstEinvoiceApplicable())
                .gstEwaybillApplicable(profile.isGstEwaybillApplicable())
                // TDS Facts
                .tdsApplicable(profile.isTdsApplicable())
                .tdsFilingFrequency(profile.getTdsFilingFrequency() != null ? profile.getTdsFilingFrequency().name() : null)
                .tdsDeductorCategory(profile.getTdsDeductorCategory() != null ? profile.getTdsDeductorCategory().name() : null)
                .tdsLowerDeductionCertificate(profile.isTdsLowerDeductionCertificate())
                // ITR Facts
                .itrApplicable(profile.isItrApplicable())
                .itrCategory(profile.getItrCategory() != null ? profile.getItrCategory().name() : null)
                .itrTaxAuditApplicable(profile.isItrTaxAuditApplicable())
                .itrTransferPricingApplicable(profile.isItrTransferPricingApplicable())
                .advanceTaxApplicable(profile.isAdvanceTaxApplicable())
                // Corporate / MCA Facts
                .mcaFilingApplicable(profile.isMcaFilingApplicable())
                // Payroll & Labour Facts
                .pfEsiApplicable(profile.isPfEsiApplicable())
                .professionalTaxApplicable(profile.isProfessionalTaxApplicable())
                .build();
    }
}
