package com.taxoryn.module.authentication.repository;

import com.taxoryn.module.authentication.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenEntity> findAllByFamilyId(UUID familyId);

    List<RefreshTokenEntity> findAllByUserIdAndRevokedAtIsNull(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshTokenEntity t SET t.revokedAt = :now, t.revokedReason = :reason WHERE t.familyId = :familyId AND t.revokedAt IS NULL")
    int revokeAllByFamilyId(@Param("familyId") UUID familyId, @Param("now") Instant now, @Param("reason") String reason);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshTokenEntity t SET t.revokedAt = :now, t.revokedReason = :reason WHERE t.userId = :userId AND t.revokedAt IS NULL")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now, @Param("reason") String reason);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshTokenEntity t SET t.revokedAt = :now, t.revokedReason = :reason WHERE t.id = :id AND t.revokedAt IS NULL")
    int revokeSingleTokenAtomic(@Param("id") UUID id, @Param("now") Instant now, @Param("reason") String reason);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RefreshTokenEntity t WHERE t.expiresAt < :cutoff")
    int deleteExpiredTokens(@Param("cutoff") Instant cutoff);
}
