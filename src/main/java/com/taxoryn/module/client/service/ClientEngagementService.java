package com.taxoryn.module.client.service;

import com.taxoryn.module.client.dto.ClientServiceDto;
import com.taxoryn.module.client.dto.CreateClientServiceRequest;
import com.taxoryn.module.client.dto.ServiceCatalogItemDto;
import com.taxoryn.module.client.dto.UpdateClientServiceRequest;
import com.taxoryn.module.client.entity.ClientServiceStatus;

import java.util.List;
import java.util.UUID;

public interface ClientEngagementService {

    List<ServiceCatalogItemDto> getServiceCatalog();

    List<ClientServiceDto> getClientServices(UUID clientId);

    ClientServiceDto getClientServiceById(UUID clientId, UUID serviceId);

    ClientServiceDto createClientService(UUID clientId, CreateClientServiceRequest request);

    ClientServiceDto updateClientService(UUID clientId, UUID serviceId, UpdateClientServiceRequest request);

    ClientServiceDto updateClientServiceStatus(UUID clientId, UUID serviceId, ClientServiceStatus status);

    void deleteClientService(UUID clientId, UUID serviceId);
}
