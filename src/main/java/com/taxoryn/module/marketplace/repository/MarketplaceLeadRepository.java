package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.EnquiryStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceLeadRepository extends JpaRepository<MarketplaceLeadEntity, UUID>, JpaSpecificationExecutor<MarketplaceLeadEntity> {

    List<MarketplaceLeadEntity> findAllByOrganizationId(UUID organizationId);

    Optional<MarketplaceLeadEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<MarketplaceLeadEntity> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<MarketplaceLeadEntity> findByReferenceNumber(String referenceNumber);

    @Query("SELECT l FROM MarketplaceLeadEntity l WHERE l.organizationId = :organizationId AND " +
           "(:status IS NULL OR l.leadStatus = :status) AND " +
           "(:search IS NULL OR LOWER(l.clientName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(l.clientEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(l.serviceCategory) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<MarketplaceLeadEntity> searchLeads(
            @Param("organizationId") UUID organizationId,
            @Param("status") MarketplaceLeadEntity.LeadStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    long countByOrganizationIdAndLeadStatus(UUID organizationId, MarketplaceLeadEntity.LeadStatus status);

    long countByOrganizationIdAndEnquiryStatus(UUID organizationId, EnquiryStatus enquiryStatus);

    long countByOrganizationId(UUID organizationId);

    List<MarketplaceLeadEntity> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Page<MarketplaceLeadEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Page<MarketplaceLeadEntity> findByCustomerIdAndEnquiryStatusOrderByCreatedAtDesc(UUID customerId, EnquiryStatus enquiryStatus, Pageable pageable);

    Page<MarketplaceLeadEntity> findByOrganizationIdAndAssignedEmployeeId(UUID organizationId, UUID assignedEmployeeId, Pageable pageable);

    long countByCustomerId(UUID customerId);

    long countByCustomerIdAndEnquiryStatus(UUID customerId, EnquiryStatus enquiryStatus);

    boolean existsByClientEmailIgnoreCaseAndTaxServiceIdAndMarketplaceProfileIdAndEnquiryStatusInAndCreatedAtAfter(
            String clientEmail,
            UUID taxServiceId,
            UUID marketplaceProfileId,
            Collection<EnquiryStatus> statuses,
            Instant after
    );
}
