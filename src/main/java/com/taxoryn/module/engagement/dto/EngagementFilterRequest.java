package com.taxoryn.module.engagement.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.engagement.model.EngagementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Engagement Query Filter Parameters")
public class EngagementFilterRequest extends PageRequestDto {

    private UUID clientId;
    private UUID locationId;
    private UUID clientServiceId;
    private UUID assignedUserId;
    private EngagementStatus status;
    private String search;
}
