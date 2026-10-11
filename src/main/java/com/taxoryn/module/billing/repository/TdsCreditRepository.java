package com.taxoryn.module.billing.repository;

import com.taxoryn.module.billing.entity.TdsCreditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TdsCreditRepository extends JpaRepository<TdsCreditEntity, UUID> {

    Optional<TdsCreditEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<TdsCreditEntity> findAllByOrganizationId(UUID organizationId);

    List<TdsCreditEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    List<TdsCreditEntity> findAllByOrganizationIdAndFinancialYear(UUID organizationId, String financialYear);
}
