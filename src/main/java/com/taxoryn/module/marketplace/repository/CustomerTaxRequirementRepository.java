package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.CustomerTaxRequirementEntity;
import com.taxoryn.module.marketplace.entity.TaxRequirementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerTaxRequirementRepository extends JpaRepository<CustomerTaxRequirementEntity, UUID> {

    @Query("SELECT r FROM CustomerTaxRequirementEntity r JOIN FETCH r.taxService ts JOIN FETCH ts.category c WHERE r.customerId = :customerId ORDER BY r.createdAt DESC")
    Page<CustomerTaxRequirementEntity> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    @Query("SELECT r FROM CustomerTaxRequirementEntity r JOIN FETCH r.taxService ts JOIN FETCH ts.category c WHERE r.customerId = :customerId AND r.status = :status ORDER BY r.createdAt DESC")
    Page<CustomerTaxRequirementEntity> findByCustomerIdAndStatus(
            @Param("customerId") UUID customerId,
            @Param("status") TaxRequirementStatus status,
            Pageable pageable
    );

    @Query("SELECT r FROM CustomerTaxRequirementEntity r JOIN FETCH r.taxService ts JOIN FETCH ts.category c WHERE r.id = :id AND r.customerId = :customerId")
    Optional<CustomerTaxRequirementEntity> findByIdAndCustomerId(@Param("id") UUID id, @Param("customerId") UUID customerId);

    @Query("SELECT r FROM CustomerTaxRequirementEntity r JOIN FETCH r.taxService ts JOIN FETCH ts.category c WHERE r.customerId = :customerId ORDER BY r.createdAt DESC")
    List<CustomerTaxRequirementEntity> findRecentByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    long countByCustomerId(UUID customerId);

    long countByCustomerIdAndStatus(UUID customerId, TaxRequirementStatus status);

    boolean existsByCustomerIdAndTaxServiceIdAndFinancialYearAndStatusIn(
            UUID customerId,
            UUID taxServiceId,
            String financialYear,
            Collection<TaxRequirementStatus> statuses
    );
}
