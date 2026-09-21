package com.taxoryn.module.employee.chat.dto;

import com.taxoryn.module.employee.chat.entity.EmployeeChatChannelEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeChatChannelDto {
    private UUID id;
    private UUID organizationId;
    private String name;
    private String displayName;
    private String description;
    private EmployeeChatChannelEntity.ChannelType channelType;
    private String department;
    private Boolean isDefault;
    private Boolean isArchived;
    private Instant createdAt;
    private String lastMessagePreview;
    private Instant lastMessageTimestamp;
    private Long unreadCount;
}
