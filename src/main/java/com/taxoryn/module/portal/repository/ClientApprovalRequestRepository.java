package com.taxoryn.module.portal.repository;

import com.taxoryn.module.portal.entity.ClientApprovalRequestEntity;
import com.taxoryn.module.portal.entity.ClientApprovalStatus;
import com.taxoryn.module.portal.entity.ClientApprovalType;
import com.taxoryn.module.portal.entity.MessageContextType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientApprovalRequestRepository extends JpaRepository<ClientApprovalRequestEntity, UUID> {

    Optional<ClientApprovalRequestEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ClientApprovalRequestEntity> findByIdAndOrganizationIdAndClientId(UUID id, UUID organizationId, UUID clientId);

    List<ClientApprovalRequestEntity> findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(UUID organizationId, UUID clientId);

    List<ClientApprovalRequestEntity> findAllByOrganizationIdAndClientIdAndStatusOrderByCreatedAtDesc(
            UUID organizationId, UUID clientId, ClientApprovalStatus status);

    List<ClientApprovalRequestEntity> findAllByOrganizationIdAndClientIdAndStatusInOrderByCreatedAtDesc(
            UUID organizationId, UUID clientId, Collection<ClientApprovalStatus> statuses);

    List<ClientApprovalRequestEntity> findAllByOrganizationIdAndClientIdAndContextTypeAndContextIdOrderByCreatedAtDesc(
            UUID organizationId, UUID clientId, MessageContextType contextType, UUID contextId);

    long countByOrganizationIdAndClientIdAndStatus(UUID organizationId, UUID clientId, ClientApprovalStatus status);

    Optional<ClientApprovalRequestEntity> findFirstByOrganizationIdAndClientIdAndApprovalTypeAndContextTypeAndContextIdAndStatus(
            UUID organizationId,
            UUID clientId,
            ClientApprovalType approvalType,
            MessageContextType contextType,
            UUID contextId,
            ClientApprovalStatus status);
}
