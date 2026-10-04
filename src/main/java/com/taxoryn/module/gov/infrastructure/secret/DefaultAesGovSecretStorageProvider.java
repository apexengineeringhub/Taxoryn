package com.taxoryn.module.gov.infrastructure.secret;

import com.taxoryn.module.gov.security.GovSecretEncryptionService;
import com.taxoryn.module.gov.spi.GovSecretStorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default secret storage provider that stores authenticated AES-256-GCM encrypted ciphertext payloads.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAesGovSecretStorageProvider implements GovSecretStorageProvider {

    public static final String STORAGE_CODE = "LOCAL_AES_GCM";

    private final GovSecretEncryptionService encryptionService;

    @Override
    public String getStorageProviderCode() {
        return STORAGE_CODE;
    }

    @Override
    public String storeSecret(UUID organizationId, UUID credentialRefId, String plaintextSecret) {
        log.debug("Encrypting secret for orgId: {}, credRefId: {}", organizationId, credentialRefId);
        return encryptionService.encrypt(plaintextSecret);
    }

    @Override
    public String retrieveSecret(UUID organizationId, UUID credentialRefId, String encryptedSecretPayload) {
        log.debug("Decrypting secret for orgId: {}, credRefId: {}", organizationId, credentialRefId);
        return encryptionService.decrypt(encryptedSecretPayload);
    }

    @Override
    public void deleteSecret(UUID organizationId, UUID credentialRefId) {
        log.debug("Secret deleted/purged for orgId: {}, credRefId: {}", organizationId, credentialRefId);
    }
}
