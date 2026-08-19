package com.taxoryn.module.employee.repository;

import com.taxoryn.module.employee.entity.EmployeeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, UUID>, JpaSpecificationExecutor<EmployeeEntity> {

    Optional<EmployeeEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<EmployeeEntity> findByOrganizationIdAndEmployeeCode(UUID organizationId, String employeeCode);

    Optional<EmployeeEntity> findByOrganizationIdAndEmail(UUID organizationId, String email);

    Optional<EmployeeEntity> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    boolean existsByOrganizationIdAndEmployeeCode(UUID organizationId, String employeeCode);

    boolean existsByOrganizationIdAndEmail(UUID organizationId, String email);

    Page<EmployeeEntity> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    List<EmployeeEntity> findAllByOrganizationId(UUID organizationId);

    List<EmployeeEntity> findAllByOrganizationIdAndStatus(UUID organizationId, EmployeeEntity.EmployeeStatus status);

    @Query("SELECT COUNT(e), " +
           "SUM(CASE WHEN e.status = com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus.ACTIVE THEN 1L ELSE 0L END) " +
           "FROM EmployeeEntity e WHERE e.organizationId = :organizationId")
    List<Object[]> getEmployeeDashboardStats(@Param("organizationId") UUID organizationId);
}
