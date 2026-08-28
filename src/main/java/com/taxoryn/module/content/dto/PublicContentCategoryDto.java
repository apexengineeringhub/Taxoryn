package com.taxoryn.module.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicContentCategoryDto {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private long publishedContentCount;
}
