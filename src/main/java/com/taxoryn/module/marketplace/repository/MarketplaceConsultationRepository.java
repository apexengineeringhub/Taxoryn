package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.MarketplaceConsultationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceConsultationRepository extends JpaRepository<MarketplaceConsultationEntity, UUID> {

    List<MarketplaceConsultationEntity> findAllByOrganizationId(UUID organizationId);

    Optional<MarketplaceConsultationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<MarketplaceConsultationEntity> findByMarketplaceProfileIdAndBookingDate(UUID marketplaceProfileId, LocalDate bookingDate);

    Page<MarketplaceConsultationEntity> findAllByOrganizationIdOrderByBookingDateDesc(UUID organizationId, Pageable pageable);

    long countByOrganizationIdAndConsultationStatus(UUID organizationId, MarketplaceConsultationEntity.ConsultationStatus status);

    List<MarketplaceConsultationEntity> findAllByCustomerIdOrderByBookingDateDesc(UUID customerId);

    long countByCustomerId(UUID customerId);
}
