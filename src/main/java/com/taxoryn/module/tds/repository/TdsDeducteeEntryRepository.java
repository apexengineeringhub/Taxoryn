package com.taxoryn.module.tds.repository;

import com.taxoryn.module.tds.entity.TdsDeducteeEntryEntity;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TdsDeducteeEntryRepository extends JpaRepository<TdsDeducteeEntryEntity, UUID> {

    Optional<TdsDeducteeEntryEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<TdsDeducteeEntryEntity> findAllByOrganizationIdAndTdsProfileId(UUID organizationId, UUID tdsProfileId);

    List<TdsDeducteeEntryEntity> findAllByOrganizationIdAndTdsReturnId(UUID organizationId, UUID tdsReturnId);

    List<TdsDeducteeEntryEntity> findAllByOrganizationIdAndChallanId(UUID organizationId, UUID challanId);

    @Query("SELECT d FROM TdsDeducteeEntryEntity d WHERE d.organizationId = :organizationId " +
           "AND (:tdsProfileId IS NULL OR d.tdsProfileId = :tdsProfileId) " +
           "AND (:tdsReturnId IS NULL OR d.tdsReturnId = :tdsReturnId) " +
           "AND (:quarter IS NULL OR d.quarter = :quarter) " +
           "AND (:financialYear IS NULL OR d.financialYear = :financialYear) " +
           "AND (:sectionCode IS NULL OR d.sectionCode = :sectionCode) " +
           "AND (:search IS NULL OR LOWER(d.deducteePan) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(d.deducteeName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TdsDeducteeEntryEntity> searchDeductees(
            @Param("organizationId") UUID organizationId,
            @Param("tdsProfileId") UUID tdsProfileId,
            @Param("tdsReturnId") UUID tdsReturnId,
            @Param("quarter") TdsQuarter quarter,
            @Param("financialYear") String financialYear,
            @Param("sectionCode") String sectionCode,
            @Param("search") String search,
            Pageable pageable
    );
}
