package com.taxoryn.module.consent.repository;

import com.taxoryn.module.consent.entity.TaxpayerConsentEntity;
import com.taxoryn.module.consent.model.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxpayerConsentRepository extends JpaRepository<TaxpayerConsentEntity, UUID>, JpaSpecificationExecutor<TaxpayerConsentEntity> {

    Optional<TaxpayerConsentEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<TaxpayerConsentEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    List<TaxpayerConsentEntity> findAllByOrganizationIdAndDelegateUserId(UUID organizationId, UUID delegateUserId);

    List<TaxpayerConsentEntity> findAllByOrganizationIdAndClientIdAndStatus(UUID organizationId, UUID clientId, ConsentStatus status);

    List<TaxpayerConsentEntity> findAllByOrganizationIdAndDelegateUserIdAndStatus(UUID organizationId, UUID delegateUserId, ConsentStatus status);

    List<TaxpayerConsentEntity> findAllByOrganizationIdAndClientIdAndDelegateUserIdAndStatus(UUID organizationId, UUID clientId, UUID delegateUserId, ConsentStatus status);

    Optional<TaxpayerConsentEntity> findByOrganizationIdAndConsentReference(UUID organizationId, String consentReference);

    long countByOrganizationIdAndStatus(UUID organizationId, ConsentStatus status);
}
