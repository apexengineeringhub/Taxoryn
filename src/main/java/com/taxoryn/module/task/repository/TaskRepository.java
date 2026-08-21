package com.taxoryn.module.task.repository;

import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, UUID>, JpaSpecificationExecutor<TaskEntity> {

    Page<TaskEntity> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Optional<TaskEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<TaskEntity> findAllByOrganizationIdAndAssignedTo(UUID organizationId, UUID assignedTo, Pageable pageable);

    Page<TaskEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId, Pageable pageable);

    java.util.List<TaskEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    long countByOrganizationIdAndClientIdAndStatusNot(UUID organizationId, UUID clientId, TaskStatus status);

    Page<TaskEntity> findAllByOrganizationIdAndStatus(UUID organizationId, TaskStatus status, Pageable pageable);

    @Query("SELECT DISTINCT t.clientId FROM TaskEntity t WHERE t.organizationId = :organizationId AND t.assignedTo IN :assigneeIds AND t.clientId IS NOT NULL")
    java.util.List<UUID> findClientIdsByAssignedToIn(@Param("organizationId") UUID organizationId, @Param("assigneeIds") Collection<UUID> assigneeIds);

    @Query("SELECT COUNT(t) FROM TaskEntity t WHERE t.organizationId = :organizationId AND t.assignedTo IN :assigneeIds AND t.status != com.taxoryn.module.task.entity.TaskEntity.TaskStatus.CANCELLED")
    long countAssignedTasks(@Param("organizationId") UUID organizationId, @Param("assigneeIds") Collection<UUID> assigneeIds);

    @Query("SELECT COUNT(t) FROM TaskEntity t WHERE t.organizationId = :organizationId AND t.assignedTo IN :assigneeIds AND t.status IN :statuses")
    long countByStatuses(@Param("organizationId") UUID organizationId, @Param("assigneeIds") Collection<UUID> assigneeIds, @Param("statuses") Collection<TaskStatus> statuses);

    @Query("SELECT COUNT(t) FROM TaskEntity t WHERE t.organizationId = :organizationId AND t.assignedTo IN :assigneeIds AND t.status IN :statuses AND t.dueDate < :currentDate")
    long countOverdueTasks(@Param("organizationId") UUID organizationId, @Param("assigneeIds") Collection<UUID> assigneeIds, @Param("statuses") Collection<TaskStatus> statuses, @Param("currentDate") LocalDate currentDate);

    /**
     * Tasks due on a specific date (e.g. today/tomorrow) that are still open, for TASK_DUE reminders.
     */
    java.util.List<TaskEntity> findAllByOrganizationIdAndDueDateAndStatusNotIn(UUID organizationId, LocalDate dueDate, Collection<TaskStatus> excludedStatuses);

    /**
     * Tasks whose due date has already passed and are still open, for TASK_OVERDUE reminders.
     */
    java.util.List<TaskEntity> findAllByOrganizationIdAndDueDateBeforeAndStatusNotIn(UUID organizationId, LocalDate currentDate, Collection<TaskStatus> excludedStatuses);

    @Query("SELECT COUNT(t), " +
           "SUM(CASE WHEN t.status IN (com.taxoryn.module.task.entity.TaskEntity.TaskStatus.TODO, com.taxoryn.module.task.entity.TaskEntity.TaskStatus.IN_PROGRESS, com.taxoryn.module.task.entity.TaskEntity.TaskStatus.UNDER_REVIEW) THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN t.status IN (com.taxoryn.module.task.entity.TaskEntity.TaskStatus.TODO, com.taxoryn.module.task.entity.TaskEntity.TaskStatus.IN_PROGRESS, com.taxoryn.module.task.entity.TaskEntity.TaskStatus.UNDER_REVIEW) AND t.dueDate < :currentDate THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN t.status = com.taxoryn.module.task.entity.TaskEntity.TaskStatus.COMPLETED THEN 1L ELSE 0L END) " +
           "FROM TaskEntity t WHERE t.organizationId = :organizationId AND t.status != com.taxoryn.module.task.entity.TaskEntity.TaskStatus.CANCELLED")
    List<Object[]> getTaskDashboardStats(@Param("organizationId") UUID organizationId, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT t.assignedTo, " +
           "COUNT(t), " +
           "SUM(CASE WHEN t.status IN (com.taxoryn.module.task.entity.TaskEntity.TaskStatus.TODO, com.taxoryn.module.task.entity.TaskEntity.TaskStatus.IN_PROGRESS, com.taxoryn.module.task.entity.TaskEntity.TaskStatus.UNDER_REVIEW) THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN t.status IN (com.taxoryn.module.task.entity.TaskEntity.TaskStatus.TODO, com.taxoryn.module.task.entity.TaskEntity.TaskStatus.IN_PROGRESS, com.taxoryn.module.task.entity.TaskEntity.TaskStatus.UNDER_REVIEW) AND t.dueDate < :currentDate THEN 1L ELSE 0L END) " +
           "FROM TaskEntity t WHERE t.organizationId = :organizationId AND t.status != com.taxoryn.module.task.entity.TaskEntity.TaskStatus.CANCELLED AND t.assignedTo IS NOT NULL " +
           "GROUP BY t.assignedTo")
    List<Object[]> getEmployeeTaskWorkloadStats(@Param("organizationId") UUID organizationId, @Param("currentDate") LocalDate currentDate);
}
