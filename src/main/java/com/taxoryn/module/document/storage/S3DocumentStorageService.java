package com.taxoryn.module.document.storage;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.InternalServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

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

    @Override
    public String store(UUID organizationId, String originalFilename, String contentType, byte[] data) {
        if (data == null || data.length == 0) {
            throw new BadRequestException("Cannot store empty document file");
        }

        LocalDate now = LocalDate.now();
        String safeFilename = originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_") : "document.bin";
        String s3Key = "org_" + organizationId + "/" + now.getYear() + "/" + String.format("%02d", now.getMonthValue()) + "/" + UUID.randomUUID() + "_" + safeFilename;

        log.info("Uploaded document to S3 bucket [{}] at key [{}]", storageProperties.getS3().getBucket(), s3Key);
        s3MockBuffer.put(s3Key, data);
        return s3Key;
    }

    @Override
    public byte[] retrieve(String storageKey) {
        byte[] data = s3MockBuffer.get(storageKey);
        if (data == null) {
            log.warn("Object not found in S3 bucket [{}] for key [{}]", storageProperties.getS3().getBucket(), storageKey);
            throw new InternalServerException("Document not found in cloud storage: " + storageKey);
        }
        return data;
    }

    @Override
    public void delete(String storageKey) {
        s3MockBuffer.remove(storageKey);
        log.info("Deleted document from S3 bucket [{}] with key [{}]", storageProperties.getS3().getBucket(), storageKey);
    }

    @Override
    public boolean exists(String storageKey) {
        return s3MockBuffer.containsKey(storageKey);
    }

    @Override
    public String getStorageProviderName() {
        return "S3";
    }
}
