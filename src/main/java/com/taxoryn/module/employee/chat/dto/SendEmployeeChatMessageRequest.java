package com.taxoryn.module.employee.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmployeeChatMessageRequest {

    @NotBlank(message = "Message body cannot be empty")
    private String messageBody;

    private String attachmentsJson;

    private UUID recipientEmployeeId;

    private UUID channelId;
}
