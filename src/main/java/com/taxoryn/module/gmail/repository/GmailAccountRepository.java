package com.taxoryn.module.gmail.repository;

import com.taxoryn.module.gmail.entity.GmailAccountEntity;
import com.taxoryn.module.gmail.entity.GmailAccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GmailAccountRepository extends JpaRepository<GmailAccountEntity, UUID>, JpaSpecificationExecutor<GmailAccountEntity> {

    Optional<GmailAccountEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<GmailAccountEntity> findByOrganizationIdAndEmailAddress(UUID organizationId, String emailAddress);

    List<GmailAccountEntity> findAllByOrganizationId(UUID organizationId);

    Page<GmailAccountEntity> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    List<GmailAccountEntity> findAllByOrganizationIdAndStatus(UUID organizationId, GmailAccountStatus status);

    List<GmailAccountEntity> findAllByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    long countByOrganizationIdAndStatus(UUID organizationId, GmailAccountStatus status);

    boolean existsByOrganizationIdAndEmailAddress(UUID organizationId, String emailAddress);
}
