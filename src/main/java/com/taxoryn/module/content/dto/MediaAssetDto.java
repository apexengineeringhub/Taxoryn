package com.taxoryn.module.content.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetDto {

    private UUID id;
    private String filename;
    private String contentType;
    private Long fileSize;
    private String publicUrl;
    private String altText;
    private String uploadedByName;
    private Instant createdAt;
}
