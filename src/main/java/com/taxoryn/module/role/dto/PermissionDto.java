package com.taxoryn.module.role.dto;

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
@Schema(description = "Permission Payload")
public class PermissionDto {

    private UUID id;
    private String code;
    private String name;
    private String module;
    private String description;
}
