package com.taxoryn.module.gst.repository;

import com.taxoryn.module.gst.entity.GstMonthlySummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GstMonthlySummaryRepository extends JpaRepository<GstMonthlySummaryEntity, UUID> {

    Optional<GstMonthlySummaryEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<GstMonthlySummaryEntity> findByOrganizationIdAndGstProfileIdAndPeriod(
            UUID organizationId, UUID gstProfileId, String period);

    List<GstMonthlySummaryEntity> findAllByOrganizationIdAndPeriod(UUID organizationId, String period);

    List<GstMonthlySummaryEntity> findAllByOrganizationIdAndGstProfileIdOrderByPeriodDesc(
            UUID organizationId, UUID gstProfileId);
}
