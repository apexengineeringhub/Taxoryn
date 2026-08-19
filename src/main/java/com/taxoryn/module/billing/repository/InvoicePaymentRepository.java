package com.taxoryn.module.billing.repository;

import com.taxoryn.module.billing.entity.InvoicePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePaymentEntity, UUID> {

    List<InvoicePaymentEntity> findAllByOrganizationIdAndInvoiceIdOrderByPaymentDateDesc(UUID organizationId, UUID invoiceId);

    List<InvoicePaymentEntity> findAllByOrganizationIdAndClientIdOrderByPaymentDateDesc(UUID organizationId, UUID clientId);
}
