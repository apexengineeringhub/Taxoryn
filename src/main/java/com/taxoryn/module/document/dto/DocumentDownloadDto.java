package com.taxoryn.module.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDownloadDto {

    private String fileName;
    private String contentType;
    private long fileSize;
    private byte[] data;
}
