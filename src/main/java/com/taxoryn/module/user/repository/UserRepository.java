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

    long countByStatus(UserEntity.UserStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT u) FROM UserEntity u JOIN u.roles r WHERE u.organizationId = :organizationId AND r.code IN ('ORG_ADMIN', 'PRACTICE_ADMIN', 'PRACTICE_OWNER') AND u.status = com.taxoryn.module.user.entity.UserEntity.UserStatus.ACTIVE")
    long countActiveOrgAdmins(@org.springframework.data.repository.query.Param("organizationId") UUID organizationId);

    @org.springframework.data.jpa.repository.Query("SELECT u.organizationId, COUNT(u.id), SUM(CASE WHEN u.status = com.taxoryn.module.user.entity.UserEntity.UserStatus.ACTIVE THEN 1L ELSE 0L END) " +
            "FROM UserEntity u WHERE u.organizationId IN :orgIds GROUP BY u.organizationId")
    java.util.List<Object[]> findUserCountsByOrganizationIds(@org.springframework.data.repository.query.Param("orgIds") java.util.List<UUID> orgIds);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM UserEntity u JOIN FETCH u.roles r " +
            "WHERE u.organizationId IN :orgIds AND r.code IN ('ORG_ADMIN', 'PRACTICE_ADMIN', 'PRACTICE_OWNER', 'TAXORYN_SUPERADMIN', 'SUPER_ADMIN') " +
            "ORDER BY u.firstName ASC")
    java.util.List<UserEntity> findAdminsByOrganizationIds(@org.springframework.data.repository.query.Param("orgIds") java.util.List<UUID> orgIds);

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT DISTINCT u FROM UserEntity u LEFT JOIN u.roles r WHERE u.organizationId = :organizationId " +
                    "AND (:status IS NULL OR u.status = :status) " +
                    "AND (:role IS NULL OR r.code = :role) " +
                    "AND (:search IS NULL OR (" +
                    "  LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR (u.phone IS NOT NULL AND u.phone LIKE CONCAT('%', :search, '%'))" +
                    "))",
            countQuery = "SELECT COUNT(DISTINCT u) FROM UserEntity u LEFT JOIN u.roles r WHERE u.organizationId = :organizationId " +
                    "AND (:status IS NULL OR u.status = :status) " +
                    "AND (:role IS NULL OR r.code = :role) " +
                    "AND (:search IS NULL OR (" +
                    "  LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR (u.phone IS NOT NULL AND u.phone LIKE CONCAT('%', :search, '%'))" +
                    "))"
    )
    Page<UserEntity> findUsersByOrganizationWithFilters(
            @org.springframework.data.repository.query.Param("organizationId") UUID organizationId,
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("role") String role,
            @org.springframework.data.repository.query.Param("status") UserEntity.UserStatus status,
            Pageable pageable
    );
}
