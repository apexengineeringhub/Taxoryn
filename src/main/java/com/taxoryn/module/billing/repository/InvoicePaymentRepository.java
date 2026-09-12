package com.taxoryn.module.billing.repository;

import com.taxoryn.module.billing.entity.InvoicePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePaymentEntity, UUID> {

    List<InvoicePaymentEntity> findAllByOrganizationId(UUID organizationId);

    List<InvoicePaymentEntity> findAllByOrganizationIdAndInvoiceIdOrderByPaymentDateDesc(UUID organizationId, UUID invoiceId);

    List<InvoicePaymentEntity> findAllByOrganizationIdAndClientIdOrderByPaymentDateDesc(UUID organizationId, UUID clientId);

    long countByOrganizationIdAndPaymentDateNotNullAndAmountNotNull(UUID organizationId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM InvoicePaymentEntity p WHERE p.organizationId = :organizationId AND p.paymentDate >= :sinceDate AND p.amount IS NOT NULL")
    java.math.BigDecimal sumAmountByOrganizationIdAndPaymentDateAfterOrEqual(
            @org.springframework.data.repository.query.Param("organizationId") UUID organizationId,
            @org.springframework.data.repository.query.Param("sinceDate") java.time.LocalDate sinceDate
    );
}
