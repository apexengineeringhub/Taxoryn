package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer onboarding information submission")
public class UpdateOnboardingDetailsRequest {

    @NotBlank(message = "Client name is required")
    private String clientName;

    private String legalName;
    private ClientType entityType;
    private String pan;
    private String gstin;
    private String tan;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
}
