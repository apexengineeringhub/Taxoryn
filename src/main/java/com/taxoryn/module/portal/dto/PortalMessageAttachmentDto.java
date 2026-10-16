package com.taxoryn.module.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalMessageAttachmentDto {
    private UUID documentId;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String downloadUrl;
}
