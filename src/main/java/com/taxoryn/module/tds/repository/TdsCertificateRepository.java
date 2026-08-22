package com.taxoryn.module.tds.repository;

import com.taxoryn.module.tds.entity.TdsCertificateEntity;
import com.taxoryn.module.tds.entity.TdsCertificateEntity.CertificateType;
import com.taxoryn.module.tds.entity.TdsCertificateEntity.DispatchStatus;
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
public interface TdsCertificateRepository extends JpaRepository<TdsCertificateEntity, UUID> {

    Optional<TdsCertificateEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<TdsCertificateEntity> findAllByOrganizationIdAndTdsProfileId(UUID organizationId, UUID tdsProfileId);

    List<TdsCertificateEntity> findAllByOrganizationIdAndTdsReturnId(UUID organizationId, UUID tdsReturnId);

    @Query("SELECT c FROM TdsCertificateEntity c WHERE c.organizationId = :organizationId " +
           "AND (:tdsProfileId IS NULL OR c.tdsProfileId = :tdsProfileId) " +
           "AND (:certificateType IS NULL OR c.certificateType = :certificateType) " +
           "AND (:quarter IS NULL OR c.quarter = :quarter) " +
           "AND (:financialYear IS NULL OR c.financialYear = :financialYear) " +
           "AND (:dispatchStatus IS NULL OR c.dispatchStatus = :dispatchStatus) " +
           "AND (:search IS NULL OR LOWER(c.deducteePan) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.deducteeName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TdsCertificateEntity> searchCertificates(
            @Param("organizationId") UUID organizationId,
            @Param("tdsProfileId") UUID tdsProfileId,
            @Param("certificateType") CertificateType certificateType,
            @Param("quarter") TdsQuarter quarter,
            @Param("financialYear") String financialYear,
            @Param("dispatchStatus") DispatchStatus dispatchStatus,
            @Param("search") String search,
            Pageable pageable
    );
}
