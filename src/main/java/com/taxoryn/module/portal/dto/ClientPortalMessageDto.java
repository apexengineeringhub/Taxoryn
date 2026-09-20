package com.taxoryn.module.portal.dto;

import com.taxoryn.module.portal.entity.PortalMessageSenderType;
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
public class ClientPortalMessageDto {

    private UUID id;
    private UUID organizationId;
    private UUID clientId;
    private PortalMessageSenderType senderType;
    private UUID senderUserId;
    private String senderName;
    private String senderEmail;
    private String messageBody;
    private String attachmentsJson;
    private boolean isReadByClient;
    private boolean isReadByPractice;
    private Instant readAt;
    private Instant createdAt;
    private Instant updatedAt;
}
