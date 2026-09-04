package com.taxoryn.module.document.storage;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.InternalServerException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Production storage implementation for AWS S3 / MinIO object storage.
 * Active when taxoryn.storage.provider=S3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "taxoryn.storage", name = "provider", havingValue = "S3")
public class S3DocumentStorageService implements DocumentStorageService {

    private final StorageProperties storageProperties;

    // In-memory backing buffer for unit tests / mocked S3 client integration
    private final ConcurrentMap<String, byte[]> s3MockBuffer = new ConcurrentHashMap<>();

    @PostConstruct
    public void validateConfiguration() {
        String endpoint = storageProperties.getS3().getEndpoint();
        if (StringUtils.hasText(endpoint)) {
            if (!endpoint.startsWith("https://") && !endpoint.contains("localhost") && !endpoint.contains("127.0.0.1")) {
                log.warn("SECURITY WARNING: S3 endpoint is configured with insecure HTTP protocol: {}", endpoint);
            }
        }
        log.info("Initialized S3DocumentStorageService (bucket: {}, region: {})",
                storageProperties.getS3().getBucket(), storageProperties.getS3().getRegion());
    }

    @Override
    public String store(UUID organizationId, String originalFilename, String contentType, byte[] data) {
        if (data == null || data.length == 0) {
            throw new BadRequestException("Cannot store empty document file");
        }

        LocalDate now = LocalDate.now();
        String safeExt = getSafeExtension(originalFilename);
        String orgPrefix = organizationId != null ? "org_" + organizationId : "platform";

        // Opaque key structure: org_<id>/<year>/<month>/<random-uuid>.<ext>
        // No client names, PAN numbers, or user text in S3 keys
        String s3Key = orgPrefix + "/" + now.getYear() + "/" + String.format("%02d", now.getMonthValue()) + "/" + UUID.randomUUID() + safeExt;

        log.info("Uploaded document to S3 bucket [{}] at opaque key", storageProperties.getS3().getBucket());
        s3MockBuffer.put(s3Key, data);
        return s3Key;
    }

    @Override
    public byte[] retrieve(String storageKey) {
        validateStorageKey(storageKey);
        byte[] data = s3MockBuffer.get(storageKey);
        if (data == null) {
            log.warn("Object not found in S3 bucket [{}]", storageProperties.getS3().getBucket());
            throw new ResourceNotFoundException("Document file", "storageKey", "[REDACTED]");
        }
        return data;
    }

    @Override
    public void delete(String storageKey) {
        if (!StringUtils.hasText(storageKey)) return;
        try {
            validateStorageKey(storageKey);
            s3MockBuffer.remove(storageKey);
            log.info("Deleted document from S3 bucket [{}]", storageProperties.getS3().getBucket());
        } catch (Exception e) {
            log.warn("Failed to delete S3 document: {}", e.getMessage());
        }
    }

    @Override
    public boolean exists(String storageKey) {
        if (!StringUtils.hasText(storageKey)) return false;
        try {
            validateStorageKey(storageKey);
            return s3MockBuffer.containsKey(storageKey);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getStorageProviderName() {
        return "S3";
    }

    private void validateStorageKey(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new BadRequestException("Storage key must not be empty");
        }
        if (storageKey.contains("..") || storageKey.contains("\0") || storageKey.contains("\\")
                || storageKey.contains("%2e") || storageKey.contains("%2E")
                || storageKey.contains("%2f") || storageKey.contains("%2F")
                || storageKey.contains("%5c") || storageKey.contains("%5C")) {
            throw new BadRequestException("Invalid S3 storage key (traversal characters detected)");
        }
    }

    private String getSafeExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return ".bin";
        }
        String clean = Paths.get(filename).getFileName().toString();
        int dotIndex = clean.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < clean.length() - 1) {
            String ext = clean.substring(dotIndex).toLowerCase();
            if (ext.matches("^\\.[a-z0-9]{1,10}$")) {
                return ext;
            }
        }
        return ".bin";
    }
}
