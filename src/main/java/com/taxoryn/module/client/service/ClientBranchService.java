package com.taxoryn.module.client.service;

import com.taxoryn.module.client.dto.ClientBranchDto;
import com.taxoryn.module.client.dto.CreateClientBranchRequest;
import com.taxoryn.module.client.dto.UpdateClientBranchRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientBranchService {

    List<ClientBranchDto> getBranches(UUID clientId, Boolean activeOnly);

    ClientBranchDto getBranchById(UUID clientId, UUID branchId);

    Optional<ClientBranchDto> getPrimaryBranch(UUID clientId);

    ClientBranchDto createBranch(UUID clientId, CreateClientBranchRequest request);

    ClientBranchDto updateBranch(UUID clientId, UUID branchId, UpdateClientBranchRequest request);

    ClientBranchDto updateStatus(UUID clientId, UUID branchId, boolean active);

    ClientBranchDto setPrimaryBranch(UUID clientId, UUID branchId);

    void deleteBranch(UUID clientId, UUID branchId);

    long getBranchesCount(UUID clientId);

    long getActiveBranchesCount(UUID clientId);
}
