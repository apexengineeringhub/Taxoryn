package com.taxoryn.module.reminder.repository;

import com.taxoryn.module.reminder.entity.ReminderEntity;
import com.taxoryn.module.reminder.entity.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<ReminderEntity, UUID>,
        JpaSpecificationExecutor<ReminderEntity> {

    // -------------------------------------------------------------------------
    // Tenant-scoped lookups
    // -------------------------------------------------------------------------

    Optional<ReminderEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    // -------------------------------------------------------------------------
    // "My Reminders" — for the current user
    // -------------------------------------------------------------------------

    List<ReminderEntity> findAllByOrganizationIdAndTargetUserIdAndStatusOrderByScheduledAtAsc(
            UUID organizationId, UUID targetUserId, ReminderStatus status);

    List<ReminderEntity> findAllByOrganizationIdAndTargetUserIdAndStatusInOrderByScheduledAtAsc(
            UUID organizationId, UUID targetUserId, java.util.Set<ReminderStatus> statuses);

    // -------------------------------------------------------------------------
    // Team Reminders — admin view
    // -------------------------------------------------------------------------

    List<ReminderEntity> findAllByOrganizationIdAndStatusOrderByScheduledAtAsc(
            UUID organizationId, ReminderStatus status);

    // -------------------------------------------------------------------------
    // Scheduler — find PENDING reminders due for processing
    // -------------------------------------------------------------------------

    /**
     * Finds all PENDING reminders whose scheduled time is at or before {@code now}
     * with notification attempts under maxAttempts.
     */
    @Query("""
            SELECT r FROM ReminderEntity r
            WHERE r.status = 'PENDING'
              AND r.scheduledAt <= :now
              AND r.notificationAttempts < :maxAttempts
            ORDER BY r.scheduledAt ASC
            """)
    List<ReminderEntity> findAllDueForProcessing(
            @Param("now") Instant now,
            @Param("maxAttempts") int maxAttempts,
            org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT r FROM ReminderEntity r
            WHERE r.status = 'PENDING'
              AND r.scheduledAt <= :now
            ORDER BY r.scheduledAt ASC
            """)
    List<ReminderEntity> findAllDueForProcessing(@Param("now") Instant now);

    /**
     * Finds all PENDING reminders for a specific organization due for processing
     * with notification attempts under maxAttempts.
     */
    @Query("""
            SELECT r FROM ReminderEntity r
            WHERE r.organizationId = :organizationId
              AND r.status = 'PENDING'
              AND r.scheduledAt <= :now
              AND r.notificationAttempts < :maxAttempts
            ORDER BY r.scheduledAt ASC
            """)
    List<ReminderEntity> findDueForOrganization(
            @Param("organizationId") UUID organizationId,
            @Param("now") Instant now,
            @Param("maxAttempts") int maxAttempts,
            org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT r FROM ReminderEntity r
            WHERE r.organizationId = :organizationId
              AND r.status = 'PENDING'
              AND r.scheduledAt <= :now
            ORDER BY r.scheduledAt ASC
            """)
    List<ReminderEntity> findDueForOrganization(@Param("organizationId") UUID organizationId,
                                                 @Param("now") Instant now);

    // -------------------------------------------------------------------------
    // Idempotency — prevent duplicate automation-generated reminders
    // -------------------------------------------------------------------------

    boolean existsByIdempotencyKey(String idempotencyKey);

    // -------------------------------------------------------------------------
    // Task association
    // -------------------------------------------------------------------------

    List<ReminderEntity> findAllByOrganizationIdAndTaskIdAndStatusIn(
            UUID organizationId, UUID taskId, java.util.Set<ReminderStatus> statuses);

    // -------------------------------------------------------------------------
    // Bulk status update for cancelled/completed tasks
    // -------------------------------------------------------------------------

    /**
     * Cancels all PENDING reminders linked to a specific task.
     * Called when a task is cancelled to avoid orphaned pending reminders.
     */
    @Modifying
    @Query("""
            UPDATE ReminderEntity r
            SET r.status = 'CANCELLED', r.cancelledAt = :cancelledAt
            WHERE r.taskId = :taskId
              AND r.organizationId = :organizationId
              AND r.status = 'PENDING'
            """)
    int cancelPendingRemindersForTask(@Param("organizationId") UUID organizationId,
                                      @Param("taskId") UUID taskId,
                                      @Param("cancelledAt") Instant cancelledAt);

    // -------------------------------------------------------------------------
    // Counts for dashboard/badge
    // -------------------------------------------------------------------------

    long countByOrganizationIdAndTargetUserIdAndStatus(UUID organizationId, UUID targetUserId, ReminderStatus status);

    long countByOrganizationIdAndTargetUserIdAndStatusAndScheduledAtBefore(
            UUID organizationId, UUID targetUserId, ReminderStatus status, Instant before);

    long countByOrganizationIdAndStatus(UUID organizationId, ReminderStatus status);

    long countByOrganizationIdAndStatusAndScheduledAtBefore(
            UUID organizationId, ReminderStatus status, Instant before);

    long countByOrganizationIdAndStatusAndScheduledAtBetween(
            UUID organizationId, ReminderStatus status, Instant start, Instant end);
}
