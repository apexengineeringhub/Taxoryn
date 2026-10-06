package com.taxoryn.module.gov.security;

import com.taxoryn.module.gov.exception.GovIntegrationException;
import com.taxoryn.module.gov.model.GovErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Enterprise AES-256-GCM authenticated encryption service for government secrets and credentials.
 * Ensures data confidentiality and integrity at rest.
 */
@Slf4j
@Component
public class GovSecretEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH_BYTE = 12;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public GovSecretEncryptionService(
            @Value("${taxoryn.gov.encryption.secret:${taxoryn.jwt.secret:taxoryn-gov-secure-credential-secret-key-32bytes}}") String secretSeed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secretSeed.getBytes(StandardCharsets.UTF_8));
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize GovSecretEncryptionService AES key", e);
        }
    }

    /**
     * Encrypts plaintext secret using AES-256-GCM.
     */
    public String encrypt(String plaintext) {
        if (!StringUtils.hasText(plaintext)) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Failed to encrypt sensitive government credential payload");
            throw new GovIntegrationException(GovErrorCode.UNKNOWN, "Encryption operation failed for government credential");
        }
    }

    /**
     * Decrypts ciphertext Base64 using AES-256-GCM.
     */
    public String decrypt(String ciphertextBase64) {
        if (!StringUtils.hasText(ciphertextBase64)) {
            return ciphertextBase64;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertextBase64);
            if (decoded.length < IV_LENGTH_BYTE) {
                throw new IllegalArgumentException("Invalid encrypted payload format");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt government credential payload");
            throw new GovIntegrationException(GovErrorCode.UNKNOWN, "Decryption operation failed for government credential");
        }
    }
}
