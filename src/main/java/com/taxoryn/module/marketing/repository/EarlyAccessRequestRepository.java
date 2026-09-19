package com.taxoryn.module.marketing.repository;

import com.taxoryn.module.marketing.entity.EarlyAccessRequestEntity;
import com.taxoryn.module.marketing.entity.EarlyAccessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EarlyAccessRequestRepository extends JpaRepository<EarlyAccessRequestEntity, UUID> {

    Optional<EarlyAccessRequestEntity> findTopByEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    Optional<EarlyAccessRequestEntity> findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(String email, EarlyAccessStatus status);

    boolean existsByEmailIgnoreCaseAndStatus(String email, EarlyAccessStatus status);

    Page<EarlyAccessRequestEntity> findByStatus(EarlyAccessStatus status, Pageable pageable);
}
