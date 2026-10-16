package com.taxoryn.module.billing.repository;

import com.taxoryn.module.billing.entity.PracticeInvoiceSeriesEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PracticeInvoiceSeriesRepository extends JpaRepository<PracticeInvoiceSeriesEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PracticeInvoiceSeriesEntity s WHERE s.organizationId = :organizationId AND s.financialYear = :financialYear AND s.seriesPrefix = :seriesPrefix")
    Optional<PracticeInvoiceSeriesEntity> findByOrgFyAndPrefixForUpdate(
            @Param("organizationId") UUID organizationId,
            @Param("financialYear") String financialYear,
            @Param("seriesPrefix") String seriesPrefix
    );

    Optional<PracticeInvoiceSeriesEntity> findByOrganizationIdAndFinancialYearAndSeriesPrefix(
            UUID organizationId, String financialYear, String seriesPrefix
    );
}
