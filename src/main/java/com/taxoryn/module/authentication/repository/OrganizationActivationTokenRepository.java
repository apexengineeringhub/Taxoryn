package com.taxoryn.module.authentication.repository;

import com.taxoryn.module.authentication.entity.OrganizationActivationTokenEntity;
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
public interface OrganizationActivationTokenRepository extends JpaRepository<OrganizationActivationTokenEntity, UUID> {

    Optional<OrganizationActivationTokenEntity> findByTokenHash(String tokenHash);

    List<OrganizationActivationTokenEntity> findAllByUserIdAndUsedAtIsNull(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE OrganizationActivationTokenEntity t SET t.usedAt = :now WHERE t.tokenHash = :tokenHash AND t.usedAt IS NULL AND t.expiresAt > :now")
    int consumeTokenAtomic(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE OrganizationActivationTokenEntity t SET t.usedAt = :now WHERE t.userId = :userId AND t.usedAt IS NULL")
    int invalidateAllPendingTokensForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM OrganizationActivationTokenEntity t WHERE t.expiresAt < :cutoff")
    int deleteExpiredTokens(@Param("cutoff") Instant cutoff);
}
