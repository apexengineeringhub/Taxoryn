package com.taxoryn.module.lead.repository;

import com.taxoryn.module.lead.entity.PracticeLeadEntity;
import com.taxoryn.module.lead.entity.PracticeLeadEntity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface PracticeLeadRepository extends JpaRepository<PracticeLeadEntity, UUID> {
    Optional<PracticeLeadEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM PracticeLeadEntity l WHERE l.id = :id AND l.organizationId = :organizationId")
    Optional<PracticeLeadEntity> findByIdAndOrganizationIdWithLock(@Param("id") UUID id, @Param("organizationId") UUID organizationId);

    @Query(value = """
      SELECT l FROM PracticeLeadEntity l
      WHERE l.organizationId = :org
        AND (:status IS NULL OR l.status = :status)
        AND (:priority IS NULL OR l.priority = :priority)
        AND (:source IS NULL OR l.source = :source)
        AND (:assigned IS NULL OR l.assignedEmployeeId = :assigned)
        AND (:serviceCode IS NULL OR l.interestedServiceCode = :serviceCode)
        AND (:search IS NULL OR (
          LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%'))
          OR (l.businessName IS NOT NULL AND LOWER(l.businessName) LIKE LOWER(CONCAT('%', :search, '%')))
          OR (l.email IS NOT NULL AND LOWER(l.email) LIKE LOWER(CONCAT('%', :search, '%')))
          OR (l.phone IS NOT NULL AND LOWER(l.phone) LIKE LOWER(CONCAT('%', :search, '%')))
        ))
        AND (:scopeEmployee IS NULL OR l.assignedEmployeeId = :scopeEmployee)
      """,
      countQuery = """
      SELECT COUNT(l) FROM PracticeLeadEntity l
      WHERE l.organizationId = :org
        AND (:status IS NULL OR l.status = :status)
        AND (:priority IS NULL OR l.priority = :priority)
        AND (:source IS NULL OR l.source = :source)
        AND (:assigned IS NULL OR l.assignedEmployeeId = :assigned)
        AND (:serviceCode IS NULL OR l.interestedServiceCode = :serviceCode)
        AND (:search IS NULL OR (
          LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%'))
          OR (l.businessName IS NOT NULL AND LOWER(l.businessName) LIKE LOWER(CONCAT('%', :search, '%')))
          OR (l.email IS NOT NULL AND LOWER(l.email) LIKE LOWER(CONCAT('%', :search, '%')))
          OR (l.phone IS NOT NULL AND LOWER(l.phone) LIKE LOWER(CONCAT('%', :search, '%')))
        ))
        AND (:scopeEmployee IS NULL OR l.assignedEmployeeId = :scopeEmployee)
      """)
    Page<PracticeLeadEntity> findLeads(@Param("org") UUID organizationId, @Param("status") LeadStatus status,
        @Param("priority") LeadPriority priority, @Param("source") LeadSource source,
        @Param("assigned") UUID assignedEmployeeId, @Param("serviceCode") String serviceCode,
        @Param("search") String search, @Param("scopeEmployee") UUID scopeEmployee, Pageable pageable);
}
