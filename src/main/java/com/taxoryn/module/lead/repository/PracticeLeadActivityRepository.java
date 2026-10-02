package com.taxoryn.module.lead.repository;

import com.taxoryn.module.lead.entity.PracticeLeadActivityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PracticeLeadActivityRepository extends JpaRepository<PracticeLeadActivityEntity, UUID> {
    List<PracticeLeadActivityEntity> findTop100ByOrganizationIdAndLeadIdOrderByOccurredAtDescCreatedAtDesc(UUID organizationId, UUID leadId);
}
