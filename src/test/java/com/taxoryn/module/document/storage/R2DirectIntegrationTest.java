package com.taxoryn.module.document.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End integration test against real Cloudflare R2 / S3 storage.
 * Active only when environment variable R2_INTEGRATION_TEST=true is set.
 */
@EnabledIfEnvironmentVariable(named = "R2_INTEGRATION_TEST", matches = "true")
class R2DirectIntegrationTest {

    @Test
    @DisplayName("R2 Live Integration: PutObject, HeadObject, GetObject, Presign, and DeleteObject lifecycle")
    void testLiveR2StorageLifecycle() {
        String accessKey = System.getenv("R2_ACCESS_KEY_ID");
        if (!StringUtils.hasText(accessKey)) accessKey = System.getenv("STORAGE_ACCESS_KEY");

        String secretKey = System.getenv("R2_SECRET_ACCESS_KEY");
        if (!StringUtils.hasText(secretKey)) secretKey = System.getenv("STORAGE_SECRET_KEY");

        String bucket = System.getenv("R2_BUCKET");
        if (!StringUtils.hasText(bucket)) bucket = System.getenv("STORAGE_BUCKET");

        String endpoint = System.getenv("R2_ENDPOINT");
        if (!StringUtils.hasText(endpoint)) endpoint = System.getenv("STORAGE_ENDPOINT");

        assertNotNull(accessKey, "R2_ACCESS_KEY_ID or STORAGE_ACCESS_KEY must be provided for live test");
        assertNotNull(secretKey, "R2_SECRET_ACCESS_KEY or STORAGE_SECRET_KEY must be provided for live test");
        assertNotNull(bucket, "R2_BUCKET or STORAGE_BUCKET must be provided for live test");

        StorageProperties properties = new StorageProperties();
        properties.setProvider("S3");
        properties.setPresignedUrlDurationMinutes(15);
        properties.getS3().setBucket(bucket);
        properties.getS3().setRegion("auto");
        properties.getS3().setAccessKey(accessKey);
        properties.getS3().setSecretKey(secretKey);
        properties.getS3().setEndpoint(endpoint);

        S3DocumentStorageService storageService = new S3DocumentStorageService(properties);
        storageService.init();

        UUID orgId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        byte[] payload = ("Taxoryn Phase 11 R2 Live Test Document: " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);

        String storageKey = null;
        try {
            // 1. Store and verify
            storageKey = storageService.store(orgId, clientId, docId, "LiveTest.txt", "text/plain", payload);
            assertNotNull(storageKey);
            assertTrue(storageKey.contains(orgId.toString()));
            assertTrue(storageKey.contains(clientId.toString()));

            // 2. Exists
            assertTrue(storageService.exists(storageKey));

            // 3. Retrieve
            byte[] retrieved = storageService.retrieve(storageKey);
            assertArrayEquals(payload, retrieved);

            // 4. Presigned URL
            String presignedUrl = storageService.generatePresignedDownloadUrl(storageKey, "LiveTest.txt", Duration.ofMinutes(15));
            assertNotNull(presignedUrl);
            assertTrue(presignedUrl.startsWith("https://"));
            assertTrue(presignedUrl.contains("X-Amz-Signature"));

        } finally {
            // 5. Cleanup
            if (storageKey != null) {
                storageService.delete(storageKey);
                assertFalse(storageService.exists(storageKey));
            }
            storageService.destroy();
        }
    }
}
