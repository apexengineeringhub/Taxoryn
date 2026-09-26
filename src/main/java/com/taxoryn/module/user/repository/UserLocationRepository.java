package com.taxoryn.module.user.repository;

import com.taxoryn.module.user.entity.UserLocationEntity;
import com.taxoryn.module.user.entity.UserLocationEntity.UserLocationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserLocationRepository extends JpaRepository<UserLocationEntity, UserLocationId> {

    @Query("SELECT ul.id.locationId FROM UserLocationEntity ul WHERE ul.id.userId = :userId AND ul.organizationId = :organizationId")
    List<UUID> findLocationIdsByUserIdAndOrganizationId(@Param("userId") UUID userId, @Param("organizationId") UUID organizationId);

    @Query("SELECT ul.id.locationId FROM UserLocationEntity ul WHERE ul.id.userId = :userId")
    List<UUID> findLocationIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT ul.id.userId FROM UserLocationEntity ul WHERE ul.id.locationId = :locationId AND ul.organizationId = :organizationId")
    List<UUID> findUserIdsByLocationIdAndOrganizationId(@Param("locationId") UUID locationId, @Param("organizationId") UUID organizationId);

    @Modifying
    @Query("DELETE FROM UserLocationEntity ul WHERE ul.id.userId = :userId AND ul.organizationId = :organizationId")
    void deleteByUserIdAndOrganizationId(@Param("userId") UUID userId, @Param("organizationId") UUID organizationId);

    @Modifying
    @Query("DELETE FROM UserLocationEntity ul WHERE ul.id.userId = :userId AND ul.id.locationId = :locationId AND ul.organizationId = :organizationId")
    void deleteByUserIdAndLocationIdAndOrganizationId(@Param("userId") UUID userId, @Param("locationId") UUID locationId, @Param("organizationId") UUID organizationId);

    boolean existsByIdUserIdAndIdLocationIdAndOrganizationId(UUID userId, UUID locationId, UUID organizationId);
}
