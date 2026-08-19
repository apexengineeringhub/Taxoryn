package com.taxoryn.module.subscription.repository;

import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {

    Optional<SubscriptionEntity> findByOrganizationId(UUID organizationId);

    boolean existsByOrganizationId(UUID organizationId);
}
