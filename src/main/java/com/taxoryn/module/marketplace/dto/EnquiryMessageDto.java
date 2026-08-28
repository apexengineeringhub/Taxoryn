package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MessageSenderType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnquiryMessageDto {
    private UUID id;
    private UUID enquiryId;
    private MessageSenderType senderType;
    private UUID senderUserId;
    private String senderName;
    private String messageBody;
    private String attachmentsJson;
    private Boolean isReadByCustomer;
    private Boolean isReadByPractice;
    private Instant readAt;
    private Instant createdAt;
}
