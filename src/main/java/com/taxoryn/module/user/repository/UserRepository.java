package com.taxoryn.module.user.repository;

import com.taxoryn.module.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByOrganizationIdAndEmailIgnoreCase(UUID organizationId, String email);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    Optional<UserEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<UserEntity> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    java.util.List<UserEntity> findAllByOrganizationId(UUID organizationId);

    java.util.List<UserEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    boolean existsByOrganizationIdAndEmailIgnoreCase(UUID organizationId, String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndClientIdIsNull(UUID organizationId);
}
