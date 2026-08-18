package com.taxoryn.module.client.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientRequest;

import java.util.UUID;

public interface ClientService {

    PagedResponse<ClientDto> getClients(PageRequestDto pageRequest);

    ClientDto getClientById(UUID clientId);

    ClientDto createClient(CreateClientRequest request);

    ClientDto updateClient(UUID clientId, UpdateClientRequest request);

    void deleteClient(UUID clientId);
}
