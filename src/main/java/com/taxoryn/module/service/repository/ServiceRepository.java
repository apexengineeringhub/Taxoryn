package com.taxoryn.module.service.repository;

import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.service.model.ServiceScope;
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

    @Query("SELECT s FROM ServiceEntity s WHERE UPPER(s.serviceCode) = UPPER(:serviceCode) AND (s.organizationId = :organizationId OR s.organizationId IS NULL) ORDER BY CASE WHEN s.organizationId = :organizationId THEN 0 ELSE 1 END")
    List<ServiceEntity> findAccessibleServicesByCodeList(@Param("serviceCode") String serviceCode, @Param("organizationId") UUID organizationId);

    default Optional<ServiceEntity> findAccessibleServiceByCode(String serviceCode, UUID organizationId) {
        List<ServiceEntity> list = findAccessibleServicesByCodeList(serviceCode, organizationId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Query("SELECT s FROM ServiceEntity s WHERE s.serviceCode IN :codes AND (s.organizationId = :organizationId OR s.organizationId IS NULL) ORDER BY CASE WHEN s.organizationId = :organizationId THEN 0 ELSE 1 END")
    List<ServiceEntity> findAccessibleServicesByCodes(@Param("codes") List<String> codes, @Param("organizationId") UUID organizationId);

    @Query("SELECT s FROM ServiceEntity s WHERE (s.organizationId = :organizationId OR s.organizationId IS NULL) ORDER BY CASE WHEN s.scope = 'TAXORYN' THEN 0 ELSE 1 END, s.serviceName ASC")
    List<ServiceEntity> findAllAccessibleServicesForTenant(@Param("organizationId") UUID organizationId);

    @Query("SELECT s FROM ServiceEntity s WHERE (s.organizationId = :organizationId OR s.organizationId IS NULL) AND s.status = 'ACTIVE' ORDER BY CASE WHEN s.scope = 'TAXORYN' THEN 0 ELSE 1 END, s.serviceName ASC")
    List<ServiceEntity> findActiveServicesForTenant(@Param("organizationId") UUID organizationId);

    List<ServiceEntity> findAllByOrganizationIdIsNullOrderByServiceNameAsc();

    List<ServiceEntity> findAllByOrganizationIdOrderByServiceNameAsc(UUID organizationId);

    Optional<ServiceEntity> findByServiceCodeAndOrganizationIdIsNull(String serviceCode);

    boolean existsByOrganizationIdAndServiceCodeIgnoreCase(UUID organizationId, String serviceCode);

    boolean existsByOrganizationIdAndServiceCode(UUID organizationId, String serviceCode);
}
