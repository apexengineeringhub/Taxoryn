package com.taxoryn.module.client.service;

import com.taxoryn.module.client.dto.ClientContactDto;
import com.taxoryn.module.client.dto.CreateClientContactRequest;
import com.taxoryn.module.client.dto.UpdateClientContactRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientContactService {

    List<ClientContactDto> getContacts(UUID clientId, Boolean activeOnly);

    ClientContactDto getContactById(UUID clientId, UUID contactId);

    Optional<ClientContactDto> getPrimaryContact(UUID clientId);

    ClientContactDto createContact(UUID clientId, CreateClientContactRequest request);

    ClientContactDto updateContact(UUID clientId, UUID contactId, UpdateClientContactRequest request);

    ClientContactDto updateStatus(UUID clientId, UUID contactId, boolean active);

    ClientContactDto setPrimaryContact(UUID clientId, UUID contactId);

    void deleteContact(UUID clientId, UUID contactId);

    long getContactsCount(UUID clientId);

    long getActiveContactsCount(UUID clientId);
}
