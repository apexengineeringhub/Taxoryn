package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.MarketplaceEnquiryMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MarketplaceEnquiryMessageRepository extends JpaRepository<MarketplaceEnquiryMessageEntity, UUID> {

    List<MarketplaceEnquiryMessageEntity> findByEnquiryIdOrderByCreatedAtAsc(UUID enquiryId);

    long countByEnquiryIdAndIsReadByCustomerFalse(UUID enquiryId);

    long countByEnquiryIdAndIsReadByPracticeFalse(UUID enquiryId);

    @Modifying
    @Query("UPDATE MarketplaceEnquiryMessageEntity m SET m.isReadByCustomer = true, m.readAt = :now WHERE m.enquiryId = :enquiryId AND m.isReadByCustomer = false")
    int markAllAsReadByCustomer(@Param("enquiryId") UUID enquiryId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE MarketplaceEnquiryMessageEntity m SET m.isReadByPractice = true, m.readAt = :now WHERE m.enquiryId = :enquiryId AND m.isReadByPractice = false")
    int markAllAsReadByPractice(@Param("enquiryId") UUID enquiryId, @Param("now") Instant now);
}
