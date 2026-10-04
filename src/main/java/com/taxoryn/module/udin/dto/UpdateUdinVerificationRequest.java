package com.taxoryn.module.udin.dto;

import com.taxoryn.module.udin.model.UdinVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update UDIN Verification Status Request")
public class UpdateUdinVerificationRequest {

    @NotNull(message = "Verification status is required")
    private UdinVerificationStatus verificationStatus;

    private String verificationSource;

    private String verificationRemarks;
}
