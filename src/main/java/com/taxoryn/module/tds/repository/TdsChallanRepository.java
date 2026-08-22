package com.taxoryn.module.tds.repository;

import com.taxoryn.module.tds.entity.TdsChallanEntity;
import com.taxoryn.module.tds.entity.TdsChallanEntity.ChallanStatus;
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
public interface TdsChallanRepository extends JpaRepository<TdsChallanEntity, UUID> {

    Optional<TdsChallanEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<TdsChallanEntity> findAllByOrganizationIdAndTdsProfileId(UUID organizationId, UUID tdsProfileId);

    List<TdsChallanEntity> findAllByOrganizationIdAndTdsReturnId(UUID organizationId, UUID tdsReturnId);

    @Query("SELECT c FROM TdsChallanEntity c WHERE c.organizationId = :organizationId " +
           "AND (:tdsProfileId IS NULL OR c.tdsProfileId = :tdsProfileId) " +
           "AND (:quarter IS NULL OR c.quarter = :quarter) " +
           "AND (:financialYear IS NULL OR c.financialYear = :financialYear) " +
           "AND (:challanStatus IS NULL OR c.challanStatus = :challanStatus) " +
           "AND (:sectionCode IS NULL OR c.sectionCode = :sectionCode) " +
           "AND (:search IS NULL OR LOWER(c.bsrCode) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.challanSerialNo) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.cin) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TdsChallanEntity> searchChallans(
            @Param("organizationId") UUID organizationId,
            @Param("tdsProfileId") UUID tdsProfileId,
            @Param("quarter") TdsQuarter quarter,
            @Param("financialYear") String financialYear,
            @Param("challanStatus") ChallanStatus challanStatus,
            @Param("sectionCode") String sectionCode,
            @Param("search") String search,
            Pageable pageable
    );

    long countByOrganizationIdAndChallanStatus(UUID organizationId, ChallanStatus challanStatus);
}
