package com.taxoryn.module.billing.repository;

import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID>, JpaSpecificationExecutor<InvoiceEntity> {

    Optional<InvoiceEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<InvoiceEntity> findByOrganizationIdAndInvoiceNumber(UUID organizationId, String invoiceNumber);

    boolean existsByOrganizationIdAndInvoiceNumber(UUID organizationId, String invoiceNumber);

    List<InvoiceEntity> findAllByOrganizationIdAndClientIdOrderByInvoiceDateDesc(UUID organizationId, UUID clientId);

    List<InvoiceEntity> findAllByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndStatus(UUID organizationId, InvoiceStatus status);

    @Query("SELECT COUNT(i) FROM InvoiceEntity i WHERE i.organizationId = :organizationId AND i.dueDate < :currentDate AND i.status IN ('ISSUED', 'PARTIALLY_PAID')")
    long countOverdueInvoices(@Param("organizationId") UUID organizationId, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT i FROM InvoiceEntity i WHERE i.organizationId = :organizationId AND i.dueDate < :currentDate AND i.status = 'ISSUED'")
    List<InvoiceEntity> findOverdueIssuedInvoices(@Param("organizationId") UUID organizationId, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT COUNT(i) FROM InvoiceEntity i WHERE i.organizationId = :organizationId")
    long countByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("SELECT " +
           "COALESCE(SUM(i.total), 0), " +
           "COALESCE(SUM(i.paidAmount), 0), " +
           "COALESCE(SUM(i.balanceDue), 0) " +
           "FROM InvoiceEntity i WHERE i.organizationId = :organizationId AND i.status != com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus.CANCELLED")
    List<Object[]> getBillingDashboardStatsSummary(@Param("organizationId") UUID organizationId);
}
