package com.taxoryn.module.service.service;

import com.taxoryn.module.service.dto.CreateServiceRequest;
import com.taxoryn.module.service.dto.ServiceDto;
import com.taxoryn.module.service.dto.UpdateServiceRequest;
import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.service.model.ServiceScope;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceCatalogService {

    List<ServiceDto> getServices();

    List<ServiceDto> getServices(String search, ServiceCategory category, ServiceScope scope, Boolean activeOnly);

    ServiceDto getServiceById(UUID id);

    Optional<ServiceDto> findServiceById(UUID id);

    ServiceDto getServiceByCode(String serviceCode);

    Optional<ServiceDto> findServiceByCode(String serviceCode);

    ServiceDto createService(CreateServiceRequest request);

    ServiceDto updateService(UUID id, UpdateServiceRequest request);

    ServiceDto toggleServiceStatus(UUID id, boolean active);

    void deleteService(UUID id);

    boolean isServiceAvailableForPractice(UUID organizationId, ServiceEntity service);
}
