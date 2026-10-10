package com.taxoryn.module.billing.repository;

import com.taxoryn.module.billing.entity.TdsCreditMatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TdsCreditMatchRepository extends JpaRepository<TdsCreditMatchEntity, UUID> {

    Optional<TdsCreditMatchEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<TdsCreditMatchEntity> findAllByOrganizationId(UUID organizationId);

    List<TdsCreditMatchEntity> findAllByOrganizationIdAndInvoiceId(UUID organizationId, UUID invoiceId);

    List<TdsCreditMatchEntity> findAllByOrganizationIdAndTdsCreditId(UUID organizationId, UUID tdsCreditId);

    void deleteAllByOrganizationId(UUID organizationId);
}
