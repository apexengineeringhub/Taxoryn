package com.taxoryn.module.client.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.client.dto.AssignClientEmployeeRequest;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.ClientFilterRequest;
import com.taxoryn.module.client.dto.ClientNoteDto;
import com.taxoryn.module.client.dto.ClientOverviewDto;
import com.taxoryn.module.client.dto.CreateClientNoteRequest;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientStatusRequest;

import java.util.List;
import java.util.UUID;

public interface ClientService {

    ClientDto createClient(CreateClientRequest request);

    ClientDto updateClient(UUID clientId, UpdateClientRequest request);

    ClientDto getClientById(UUID clientId);

    PagedResponse<ClientDto> getClients(ClientFilterRequest filterRequest);

    ClientDto updateClientStatus(UUID clientId, UpdateClientStatusRequest request);

    ClientDto assignEmployee(UUID clientId, AssignClientEmployeeRequest request);

    void deleteClient(UUID clientId);

    ClientOverviewDto getClientOverview(UUID clientId);

    ClientNoteDto addClientNote(UUID clientId, CreateClientNoteRequest request);

    List<ClientNoteDto> getClientNotes(UUID clientId);
}
