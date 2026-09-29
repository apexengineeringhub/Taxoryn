package com.taxoryn.module.organization.repository;

import com.taxoryn.module.organization.entity.EmployeeLocationEntity;
import com.taxoryn.module.organization.entity.EmployeeLocationEntity.EmployeeLocationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeLocationRepository extends JpaRepository<EmployeeLocationEntity, EmployeeLocationId> {

    @Query("SELECT el.id.employeeId FROM EmployeeLocationEntity el WHERE el.id.locationId = :locationId")
    List<UUID> findEmployeeIdsByLocationId(@Param("locationId") UUID locationId);

    @Query("SELECT el.id.locationId FROM EmployeeLocationEntity el WHERE el.id.employeeId = :employeeId")
    List<UUID> findLocationIdsByEmployeeId(@Param("employeeId") UUID employeeId);

    @Modifying
    @Query("DELETE FROM EmployeeLocationEntity el WHERE el.id.locationId = :locationId")
    void deleteByLocationId(@Param("locationId") UUID locationId);

    @Modifying
    @Query("DELETE FROM EmployeeLocationEntity el WHERE el.id.employeeId = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") UUID employeeId);

    @Modifying
    @Query("DELETE FROM EmployeeLocationEntity el WHERE el.id.employeeId = :employeeId AND el.id.locationId = :locationId")
    void deleteByEmployeeIdAndLocationId(@Param("employeeId") UUID employeeId, @Param("locationId") UUID locationId);

    boolean existsById_EmployeeIdAndId_LocationId(UUID employeeId, UUID locationId);
}
