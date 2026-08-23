package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practice Service Association DTO")
public class PracticeServiceDto {

    private UUID id;
    private UUID marketplaceProfileId;
    private UUID taxServiceId;
    private String taxServiceCode;
    private String taxServiceName;
    private String categoryCode;
    private String categoryName;
    private String description;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
