package com.taxoryn.module.employee.chat.repository;

import com.taxoryn.module.employee.chat.entity.EmployeeChatChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeChatChannelRepository extends JpaRepository<EmployeeChatChannelEntity, UUID> {

    List<EmployeeChatChannelEntity> findAllByOrganizationIdAndIsArchivedFalseOrderByCreatedAtAsc(UUID organizationId);

    Optional<EmployeeChatChannelEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<EmployeeChatChannelEntity> findByOrganizationIdAndNameAndIsArchivedFalse(UUID organizationId, String name);

    @Query("SELECT c FROM EmployeeChatChannelEntity c WHERE c.organizationId = :organizationId AND c.isArchived = false AND (c.channelType = 'GENERAL' OR (c.channelType = 'DEPARTMENT' AND LOWER(c.department) = LOWER(:department))) ORDER BY c.createdAt ASC")
    List<EmployeeChatChannelEntity> findAccessibleChannelsForDepartment(
            @Param("organizationId") UUID organizationId,
            @Param("department") String department
    );

    @Query("SELECT c FROM EmployeeChatChannelEntity c WHERE c.organizationId = :organizationId AND c.isArchived = false AND c.channelType = 'GENERAL' ORDER BY c.createdAt ASC")
    List<EmployeeChatChannelEntity> findGeneralChannels(@Param("organizationId") UUID organizationId);
}
