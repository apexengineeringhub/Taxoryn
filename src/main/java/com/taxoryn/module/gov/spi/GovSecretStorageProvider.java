package com.taxoryn.module.gov.spi;

import java.util.UUID;

/**
 * SPI for storing, retrieving, and invalidating encrypted government secrets.
 * Decouples the domain/connection layer from the specific storage mechanism (AES-GCM DB, Vault, KMS).
 */
public interface GovSecretStorageProvider {

    /**
     * Unique identifier for this storage provider strategy (e.g. "LOCAL_AES_GCM", "AWS_SECRETS_MANAGER").
     */
    String getStorageProviderCode();

    /**
     * Encrypts and securely packages the plaintext secret for storage.
     */
    String storeSecret(UUID organizationId, UUID credentialRefId, String plaintextSecret);

    /**
     * Decrypts and retrieves the plaintext secret.
     */
    String retrieveSecret(UUID organizationId, UUID credentialRefId, String encryptedSecretPayload);

    /**
     * Deletes or purges the secret from the underlying storage mechanism.
     */
    void deleteSecret(UUID organizationId, UUID credentialRefId);
}
