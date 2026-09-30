package com.taxoryn.module.service.service;

import com.taxoryn.module.service.dto.CreateServiceRequest;
import com.taxoryn.module.service.dto.ServiceDto;
import com.taxoryn.module.service.dto.UpdateServiceRequest;
import com.taxoryn.module.service.entity.ServiceEntity;

import java.util.List;
import java.util.UUID;

public interface ServiceCatalogService {

    List<ServiceDto> getServices();

    ServiceDto getServiceById(UUID id);

    ServiceDto getServiceByCode(String serviceCode);

    ServiceDto createService(CreateServiceRequest request);

    ServiceDto updateService(UUID id, UpdateServiceRequest request);

    boolean isServiceAvailableForPractice(UUID organizationId, ServiceEntity service);
}
