package com.taxoryn.module.moduleconfig.dto;

import com.taxoryn.module.moduleconfig.model.ProductModuleCategory;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductModuleDto {

    private UUID id;
    private ProductModuleCode code;
    private String name;
    private String description;
    private ProductModuleCategory category;
    private String status;
    private boolean enabledByDefault;
    private int displayOrder;
}
