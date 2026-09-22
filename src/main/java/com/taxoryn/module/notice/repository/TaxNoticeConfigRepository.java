package com.taxoryn.module.notice.repository;

import com.taxoryn.module.notice.entity.TaxNoticeConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxNoticeConfigRepository extends JpaRepository<TaxNoticeConfigEntity, UUID> {

    Optional<TaxNoticeConfigEntity> findByOrganizationId(UUID organizationId);

    boolean existsByOrganizationId(UUID organizationId);

    void deleteByOrganizationId(UUID organizationId);
}
