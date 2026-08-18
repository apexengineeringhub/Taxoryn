package com.taxoryn.module.client.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.mapper.ClientMapper;
import com.taxoryn.module.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ClientDto> getClients(PageRequestDto pageRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Page<ClientEntity> page = clientRepository.findAllByOrganizationId(organizationId, pageRequest.toPageable());
        return PagedResponse.of(page, clientMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDto getClientById(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));
        return clientMapper.toDto(client);
    }

    @Override
    @Transactional
    public ClientDto createClient(CreateClientRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        ClientEntity client = ClientEntity.builder()
                .clientType(request.getClientType())
                .displayName(request.getDisplayName().trim())
                .legalName(request.getLegalName())
                .pan(request.getPan())
                .gstin(request.getGstin())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(ClientStatus.ACTIVE)
                .build();
        client.setOrganizationId(organizationId);

        ClientEntity saved = clientRepository.save(client);
        log.info("Created client {} for organization {}", saved.getId(), organizationId);
        return clientMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ClientDto updateClient(UUID clientId, UpdateClientRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        if (request.getClientType() != null) client.setClientType(request.getClientType());
        if (request.getDisplayName() != null) client.setDisplayName(request.getDisplayName().trim());
        if (request.getLegalName() != null) client.setLegalName(request.getLegalName());
        if (request.getPan() != null) client.setPan(request.getPan());
        if (request.getGstin() != null) client.setGstin(request.getGstin());
        if (request.getEmail() != null) client.setEmail(request.getEmail());
        if (request.getPhone() != null) client.setPhone(request.getPhone());
        if (request.getStatus() != null) client.setStatus(request.getStatus());

        ClientEntity saved = clientRepository.save(client);
        log.info("Updated client {} for organization {}", saved.getId(), organizationId);
        return clientMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteClient(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        client.setStatus(ClientStatus.ARCHIVED);
        clientRepository.save(client);
        log.info("Archived client {} for organization {}", clientId, organizationId);
    }
}
