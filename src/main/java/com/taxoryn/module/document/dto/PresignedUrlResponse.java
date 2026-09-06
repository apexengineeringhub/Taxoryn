package com.taxoryn.module.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponse {

    /**
     * Presigned short-lived URL (for S3/R2) or authenticated relative stream endpoint (for LOCAL).
     */
    private String downloadUrl;

    /**
     * Expiration duration in seconds (0 for local static/stream endpoints).
     */
    private long expiresInSeconds;

    /**
     * Exact UTC timestamp when this download URL expires (null for local streaming endpoints).
     */
    private Instant expiresAt;

    /**
     * Cleaned filename of the document.
     */
    private String fileName;

    /**
     * MIME type of the document.
     */
    private String contentType;

    /**
     * File size in bytes.
     */
    private long fileSize;

    /**
     * Active storage provider ("S3" or "LOCAL").
     */
    private String provider;
}
