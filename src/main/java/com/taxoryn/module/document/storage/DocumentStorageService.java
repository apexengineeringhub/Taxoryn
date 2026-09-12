package com.taxoryn.module.document.storage;

import java.time.Duration;
import java.util.UUID;

/**
 * Storage service interface for document persistence.
 * Implementations provide tenant-isolated file operations across Local Filesystem and S3/R2 object storage.
 */
public interface DocumentStorageService {

    /**
     * Store raw document binary data in the storage backend.
     *
     * @param organizationId Tenant organization ID
     * @param originalFilename Original name of the uploaded file
     * @param contentType MIME type of the content
     * @param data Binary file content
     * @return Unique storage key used to retrieve or delete the document
     */
    String store(UUID organizationId, String originalFilename, String contentType, byte[] data);

    /**
     * Store raw document binary data with full tenant, client, and document context.
     * Default implementation delegates to {@link #store(UUID, String, String, byte[])}.
     *
     * @param organizationId Tenant organization ID
     * @param clientId Client ID (optional)
     * @param documentId Document ID
     * @param originalFilename Original name of the uploaded file
     * @param contentType MIME type of the content
     * @param data Binary file content
     * @return Unique storage key used to retrieve or delete the document
     */
    default String store(UUID organizationId, UUID clientId, UUID documentId, String originalFilename, String contentType, byte[] data) {
        return store(organizationId, originalFilename, contentType, data);
    }

    /**
     * Store document content directly from a local path/temporary file without loading into heap memory.
     *
     * @param organizationId Tenant organization ID
     * @param clientId Client ID (optional)
     * @param documentId Document ID (optional)
     * @param originalFilename Original name of the uploaded file
     * @param contentType MIME type of the content
     * @param sourceFile Path to source file
     * @return Unique storage key
     */
    default String store(UUID organizationId, UUID clientId, UUID documentId, String originalFilename, String contentType, java.nio.file.Path sourceFile) {
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(sourceFile);
            return store(organizationId, clientId, documentId, originalFilename, contentType, bytes);
        } catch (java.io.IOException e) {
            throw new com.taxoryn.core.exception.InternalServerException("Failed to read source file for storage: " + e.getMessage());
        }
    }

    /**
     * Store document content from an input stream with known content length.
     *
     * @param organizationId Tenant organization ID
     * @param clientId Client ID (optional)
     * @param documentId Document ID (optional)
     * @param originalFilename Original name of the uploaded file
     * @param contentType MIME type of the content
     * @param inputStream Input stream
     * @param contentLength Size of stream in bytes
     * @return Unique storage key
     */
    default String store(UUID organizationId, UUID clientId, UUID documentId, String originalFilename, String contentType, java.io.InputStream inputStream, long contentLength) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            return store(organizationId, clientId, documentId, originalFilename, contentType, bytes);
        } catch (java.io.IOException e) {
            throw new com.taxoryn.core.exception.InternalServerException("Failed to read input stream for storage: " + e.getMessage());
        }
    }

    /**
     * Retrieve document binary data by storage key.
     *
     * @param storageKey Unique storage key
     * @return Binary file content
     */
    byte[] retrieve(String storageKey);

    /**
     * Delete document from storage backend.
     *
     * @param storageKey Unique storage key
     */
    void delete(String storageKey);

    /**
     * Check if document exists in storage.
     *
     * @param storageKey Unique storage key
     * @return true if exists, false otherwise
     */
    boolean exists(String storageKey);

    /**
     * Returns the name of the active storage provider (e.g. LOCAL, S3).
     */
    String getStorageProviderName();

    /**
     * Indicates whether this storage backend supports generating short-lived presigned URLs.
     *
     * @return true for S3/R2 providers, false for local filesystem storage
     */
    default boolean supportsPresignedUrls() {
        return false;
    }

    /**
     * Generates a short-lived presigned download URL with Content-Disposition header.
     *
     * @param storageKey Object storage key
     * @param originalFilename Original file name for download attachment naming
     * @param expiration Expiration duration
     * @return Full signed download URL
     */
    default String generatePresignedDownloadUrl(String storageKey, String originalFilename, Duration expiration) {
        throw new UnsupportedOperationException("Presigned URLs are not supported by storage provider: " + getStorageProviderName());
    }
}
