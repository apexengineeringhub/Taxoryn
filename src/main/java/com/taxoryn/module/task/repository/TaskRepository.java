package com.taxoryn.module.task.repository;

import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {

    Page<TaskEntity> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Optional<TaskEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<TaskEntity> findAllByOrganizationIdAndAssignedTo(UUID organizationId, UUID assignedTo, Pageable pageable);

    Page<TaskEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId, Pageable pageable);

    Page<TaskEntity> findAllByOrganizationIdAndStatus(UUID organizationId, TaskStatus status, Pageable pageable);

    @Query("SELECT COUNT(t) FROM TaskEntity t WHERE t.organizationId = :organizationId AND t.assignedTo IN :assigneeIds AND t.status != com.taxoryn.module.task.entity.TaskEntity.TaskStatus.CANCELLED")
    long countAssignedTasks(@Param("organizationId") UUID organizationId, @Param("assigneeIds") Collection<UUID> assigneeIds);

    @Query("SELECT COUNT(t) FROM TaskEntity t WHERE t.organizationId = :organizationId AND t.assignedTo IN :assigneeIds AND t.status IN :statuses")
    long countByStatuses(@Param("organizationId") UUID organizationId, @Param("assigneeIds") Collection<UUID> assigneeIds, @Param("statuses") Collection<TaskStatus> statuses);

    @Query("SELECT COUNT(t) FROM TaskEntity t WHERE t.organizationId = :organizationId AND t.assignedTo IN :assigneeIds AND t.status IN :statuses AND t.dueDate < :currentDate")
    long countOverdueTasks(@Param("organizationId") UUID organizationId, @Param("assigneeIds") Collection<UUID> assigneeIds, @Param("statuses") Collection<TaskStatus> statuses, @Param("currentDate") LocalDate currentDate);
}
