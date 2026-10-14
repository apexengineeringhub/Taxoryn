package com.taxoryn.module.portal.dto;

import com.taxoryn.module.portal.entity.MessageContextType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendClientPortalMessageRequest {

    @NotBlank(message = "Message body cannot be blank")
    @Size(max = 4000, message = "Message body cannot exceed 4000 characters")
    private String messageBody;

    private MessageContextType contextType;
    private UUID contextId;
    private String contextTitle;
    private UUID parentMessageId;

    private List<PortalMessageAttachmentDto> attachments;
    private String attachmentsJson;
}
