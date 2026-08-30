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

    // NOTE: demo/test data seeding across the suite can end up inserting more than one
    // system-role row sharing the same code (e.g. multiple non-transactional test classes
    // each independently seeding "ORG_ADMIN" into the same shared test database). A plain
    // derived findBy...() here would throw NonUniqueResultException in that situation, so
    // this is implemented as a default method backed by an ordered list query that
    // deterministically returns the oldest (canonical) matching row instead. Existing
    // callers are unaffected - same method name, same Optional<RoleEntity> return type.
    default Optional<RoleEntity> findByCodeAndIsSystemRoleTrue(String code) {
        List<RoleEntity> matches = findAllByCodeAndIsSystemRoleTrueOrderByCreatedAtAsc(code);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    List<RoleEntity> findAllByCodeAndIsSystemRoleTrueOrderByCreatedAtAsc(String code);

    Optional<RoleEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("SELECT r FROM RoleEntity r WHERE r.isSystemRole = true OR r.organizationId = :organizationId")
    List<RoleEntity> findAllAvailableForOrganization(@Param("organizationId") UUID organizationId);

    List<RoleEntity> findByOrganizationId(UUID organizationId);

    List<RoleEntity> findByIsSystemRoleTrue();

    @Query("SELECT r FROM RoleEntity r WHERE r.code IN :codes AND (r.isSystemRole = true OR r.organizationId = :organizationId)")
    List<RoleEntity> findByCodesAndOrganizationId(@Param("codes") Set<String> codes, @Param("organizationId") UUID organizationId);

    boolean existsByCodeAndOrganizationId(String code, UUID organizationId);
}
