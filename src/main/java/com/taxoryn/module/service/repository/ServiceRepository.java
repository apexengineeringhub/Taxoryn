package com.taxoryn.module.service.repository;

import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.service.model.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, UUID>, JpaSpecificationExecutor<ServiceEntity> {

    @Query("SELECT s FROM ServiceEntity s WHERE s.id = :id AND (s.organizationId = :organizationId OR s.organizationId IS NULL)")
    Optional<ServiceEntity> findAccessibleServiceById(@Param("id") UUID id, @Param("organizationId") UUID organizationId);

    @Query("SELECT s FROM ServiceEntity s WHERE s.serviceCode = :serviceCode AND (s.organizationId = :organizationId OR s.organizationId IS NULL)")
    Optional<ServiceEntity> findAccessibleServiceByCode(@Param("serviceCode") String serviceCode, @Param("organizationId") UUID organizationId);

    @Query("SELECT s FROM ServiceEntity s WHERE (s.organizationId = :organizationId OR s.organizationId IS NULL) AND s.status = 'ACTIVE' ORDER BY s.serviceName ASC")
    List<ServiceEntity> findActiveServicesForTenant(@Param("organizationId") UUID organizationId);

    List<ServiceEntity> findAllByOrganizationIdIsNullOrderByServiceNameAsc();

    Optional<ServiceEntity> findByServiceCodeAndOrganizationIdIsNull(String serviceCode);

    boolean existsByOrganizationIdAndServiceCode(UUID organizationId, String serviceCode);
}
