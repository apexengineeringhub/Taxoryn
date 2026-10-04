package com.taxoryn.module.gov.service;

import com.taxoryn.module.gov.model.GovProviderType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Utility for generating deterministic SHA-256 idempotency keys.
 */
public final class GovIdempotencyKeyGenerator {

    private GovIdempotencyKeyGenerator() {
    }

    /**
     * Computes a deterministic SHA-256 idempotency key from tenant and operation coordinates.
     */
    public static String generateKey(
            UUID organizationId,
            GovProviderType providerType,
            String businessEntityType,
            UUID businessEntityId,
            String operationType,
            String payloadFingerprint) {

        String raw = String.format("%s:%s:%s:%s:%s:%s",
                organizationId != null ? organizationId.toString() : "GLOBAL",
                providerType != null ? providerType.name() : "UNKNOWN",
                businessEntityType != null ? businessEntityType : "",
                businessEntityId != null ? businessEntityId.toString() : "",
                operationType != null ? operationType : "",
                payloadFingerprint != null ? payloadFingerprint : "");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable in JVM runtime", e);
        }
    }
}
