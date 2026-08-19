package com.taxoryn.module.document.storage;

import java.util.UUID;

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
}
