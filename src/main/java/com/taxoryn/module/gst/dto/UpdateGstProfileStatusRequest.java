package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gst.entity.GstProfileEntity.GstProfileStatus;
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
@Schema(description = "Update GST Profile Lifecycle Status Payload")
public class UpdateGstProfileStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Target GST status", example = "ACTIVE")
    private GstProfileStatus status;
}
