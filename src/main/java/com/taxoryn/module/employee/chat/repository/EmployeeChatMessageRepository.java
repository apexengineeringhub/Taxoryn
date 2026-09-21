package com.taxoryn.module.employee.chat.repository;

import com.taxoryn.module.employee.chat.entity.EmployeeChatMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeChatMessageRepository extends JpaRepository<EmployeeChatMessageEntity, UUID> {

    @Query("SELECT m FROM EmployeeChatMessageEntity m WHERE m.organizationId = :organizationId AND m.channelId IS NULL " +
           "AND ((m.senderEmployeeId = :empA AND m.recipientEmployeeId = :empB) OR (m.senderEmployeeId = :empB AND m.recipientEmployeeId = :empA)) " +
           "ORDER BY m.createdAt ASC")
    List<EmployeeChatMessageEntity> findDirectMessagesBetween(
            @Param("organizationId") UUID organizationId,
            @Param("empA") UUID empA,
            @Param("empB") UUID empB
    );

    @Query("SELECT m FROM EmployeeChatMessageEntity m WHERE m.organizationId = :organizationId AND m.channelId = :channelId ORDER BY m.createdAt ASC")
    List<EmployeeChatMessageEntity> findChannelMessages(
            @Param("organizationId") UUID organizationId,
            @Param("channelId") UUID channelId
    );

    @Query("SELECT m FROM EmployeeChatMessageEntity m WHERE m.organizationId = :organizationId AND m.channelId IS NULL " +
           "AND ((m.senderEmployeeId = :empA AND m.recipientEmployeeId = :empB) OR (m.senderEmployeeId = :empB AND m.recipientEmployeeId = :empA)) " +
           "ORDER BY m.createdAt DESC")
    List<EmployeeChatMessageEntity> findLatestDirectMessage(
            @Param("organizationId") UUID organizationId,
            @Param("empA") UUID empA,
            @Param("empB") UUID empB,
            Pageable pageable
    );

    @Query("SELECT m FROM EmployeeChatMessageEntity m WHERE m.organizationId = :organizationId AND m.channelId = :channelId ORDER BY m.createdAt DESC")
    List<EmployeeChatMessageEntity> findLatestChannelMessage(
            @Param("organizationId") UUID organizationId,
            @Param("channelId") UUID channelId,
            Pageable pageable
    );

    @Query("SELECT COUNT(m) FROM EmployeeChatMessageEntity m WHERE m.organizationId = :organizationId AND m.recipientEmployeeId = :recipientId AND m.isRead = false")
    long countTotalUnreadForRecipient(
            @Param("organizationId") UUID organizationId,
            @Param("recipientId") UUID recipientId
    );

    @Query("SELECT COUNT(m) FROM EmployeeChatMessageEntity m WHERE m.organizationId = :organizationId AND m.senderEmployeeId = :senderId AND m.recipientEmployeeId = :recipientId AND m.isRead = false")
    long countUnreadDirectFromSender(
            @Param("organizationId") UUID organizationId,
            @Param("senderId") UUID senderId,
            @Param("recipientId") UUID recipientId
    );

    @Modifying
    @Query("UPDATE EmployeeChatMessageEntity m SET m.isRead = true, m.readAt = :readAt WHERE m.organizationId = :organizationId AND m.senderEmployeeId = :senderId AND m.recipientEmployeeId = :recipientId AND m.isRead = false")
    int markDirectMessagesRead(
            @Param("organizationId") UUID organizationId,
            @Param("senderId") UUID senderId,
            @Param("recipientId") UUID recipientId,
            @Param("readAt") Instant readAt
    );
}
