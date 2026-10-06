package com.taxoryn.module.gov.auth.dto;

import com.taxoryn.module.gov.auth.model.GovAuthPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to initiate an EVC challenge flow")
public class GovEvcChallengeRequest {

    @NotNull(message = "Connection ID is required")
    @Schema(description = "Government Connection ID")
    private UUID connectionId;

    @NotNull(message = "Purpose is required")
    @Schema(description = "EVC purpose scope (ITR_EVC or TDS_EVC)")
    private GovAuthPurpose purpose;

    @Schema(description = "Client or correlation identifier")
    private String correlationId;

    @Schema(description = "Domain entity ID (e.g. Return ID, Profile ID)")
    private String domainEntityId;

    @Schema(description = "Provider options / mock directives")
    private Map<String, Object> options;
}
