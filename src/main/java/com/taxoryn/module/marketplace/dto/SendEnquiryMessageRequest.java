package com.taxoryn.module.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEnquiryMessageRequest {

    @NotBlank(message = "Message body is required")
    @Size(max = 4000, message = "Message body must not exceed 4000 characters")
    private String messageBody;

    private String attachmentsJson;
}
