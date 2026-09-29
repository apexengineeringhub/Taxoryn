package com.taxoryn.module.engagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Engagement Assignment Request Payload")
public class UpdateEngagementAssignmentRequest {

    private UUID assignedUserId;
    private UUID reviewerUserId;
    private String notes;
}
