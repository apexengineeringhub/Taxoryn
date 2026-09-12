package com.taxoryn.module.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDownloadDto {

    private String fileName;
    private String contentType;
    private long fileSize;
    private byte[] data;
    private StreamingResponseBody stream;
}
