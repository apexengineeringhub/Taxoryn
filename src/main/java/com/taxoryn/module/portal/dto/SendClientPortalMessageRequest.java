package com.taxoryn.module.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendClientPortalMessageRequest {

    @NotBlank(message = "Message body cannot be blank")
    @Size(max = 4000, message = "Message body cannot exceed 4000 characters")
    private String messageBody;

    private String attachmentsJson;
}
