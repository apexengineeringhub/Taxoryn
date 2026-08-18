package com.taxoryn.module.role.repository;

import com.taxoryn.module.role.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByCodeAndOrganizationId(String code, UUID organizationId);

    Optional<RoleEntity> findByCodeAndIsSystemRoleTrue(String code);

    Optional<RoleEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("SELECT r FROM RoleEntity r WHERE r.isSystemRole = true OR r.organizationId = :organizationId")
    List<RoleEntity> findAllAvailableForOrganization(@Param("organizationId") UUID organizationId);

    List<RoleEntity> findByOrganizationId(UUID organizationId);

    List<RoleEntity> findByIsSystemRoleTrue();

    @Query("SELECT r FROM RoleEntity r WHERE r.code IN :codes AND (r.isSystemRole = true OR r.organizationId = :organizationId)")
    List<RoleEntity> findByCodesAndOrganizationId(@Param("codes") Set<String> codes, @Param("organizationId") UUID organizationId);

    boolean existsByCodeAndOrganizationId(String code, UUID organizationId);
}
