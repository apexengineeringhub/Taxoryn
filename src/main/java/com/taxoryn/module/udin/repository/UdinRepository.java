package com.taxoryn.module.udin.repository;

import com.taxoryn.module.udin.entity.UdinEntity;
import com.taxoryn.module.udin.model.UdinDocumentType;
import com.taxoryn.module.udin.model.UdinStatus;
import com.taxoryn.module.udin.model.UdinVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UdinRepository extends JpaRepository<UdinEntity, UUID>, JpaSpecificationExecutor<UdinEntity> {

    Optional<UdinEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<UdinEntity> findByOrganizationIdAndUdin(UUID organizationId, String udin);

    boolean existsByOrganizationIdAndUdin(UUID organizationId, String udin);

    List<UdinEntity> findByOrganizationId(UUID organizationId);

    List<UdinEntity> findByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndStatus(UUID organizationId, UdinStatus status);

    long countByOrganizationIdAndVerificationStatus(UUID organizationId, UdinVerificationStatus verificationStatus);
}
