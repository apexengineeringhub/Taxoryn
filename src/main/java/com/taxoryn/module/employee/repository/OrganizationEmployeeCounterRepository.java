package com.taxoryn.module.employee.repository;

import com.taxoryn.module.employee.entity.OrganizationEmployeeCounterEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationEmployeeCounterRepository extends JpaRepository<OrganizationEmployeeCounterEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM OrganizationEmployeeCounterEntity c WHERE c.organizationId = :organizationId")
    Optional<OrganizationEmployeeCounterEntity> findByOrganizationIdForUpdate(@Param("organizationId") UUID organizationId);
}
