package com.taxoryn.module.udin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cancel/Revoke UDIN Record Request")
public class CancelUdinRequest {

    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
