package com.taxoryn.module.document.storage;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class S3DocumentStorageServiceTest {

    private StorageProperties storageProperties;
    private S3DocumentStorageService storageService;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setProvider("S3");
        storageProperties.setPresignedUrlDurationMinutes(15);
        storageProperties.getS3().setBucket("test-taxoryn-bucket");
        storageProperties.getS3().setRegion("ap-south-1");
        storageProperties.getS3().setAccessKey("test-access-key");
        storageProperties.getS3().setSecretKey("test-secret-key-12345");
        storageProperties.getS3().setEndpoint("https://test-account.r2.cloudflarestorage.com");

        storageService = new S3DocumentStorageService(storageProperties);
        storageService.init();
    }

    @Test
    @DisplayName("S3 Storage: Provider name is S3 and supports presigned URLs")
    void testStorageProviderMetadata() {
        assertEquals("S3", storageService.getStorageProviderName());
        assertTrue(storageService.supportsPresignedUrls());
    }

    @Test
    @DisplayName("S3 Storage: Store and retrieve document with structured tenant isolation key")
    void testStoreAndRetrieveStructuredDocument() {
        UUID orgId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        byte[] content = "Audit Report PDF Data".getBytes(StandardCharsets.UTF_8);

        String key = storageService.store(orgId, clientId, docId, "AuditReport.pdf", "application/pdf", content);

        assertNotNull(key);
        assertTrue(key.startsWith("tenants/org_" + orgId + "/clients/" + clientId + "/documents/" + docId));
        assertTrue(key.endsWith(".pdf"));

        assertTrue(storageService.exists(key));
        byte[] retrieved = storageService.retrieve(key);
        assertArrayEquals(content, retrieved);
    }

    @Test
    @DisplayName("S3 Storage: Store document without client ID uses tenant-level documents path")
    void testStoreDocumentWithoutClient() {
        UUID orgId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        byte[] content = "Tenant Policy Statement".getBytes(StandardCharsets.UTF_8);

        String key = storageService.store(orgId, null, docId, "Policy.pdf", "application/pdf", content);

        assertNotNull(key);
        assertTrue(key.startsWith("tenants/org_" + orgId + "/documents/" + docId));
        assertTrue(key.endsWith(".pdf"));
        assertTrue(storageService.exists(key));
    }

    @Test
    @DisplayName("S3 Storage: Delete removes document from storage")
    void testDeleteDocument() {
        UUID orgId = UUID.randomUUID();
        byte[] content = "Data to be deleted".getBytes(StandardCharsets.UTF_8);
        String key = storageService.store(orgId, "temp.txt", "text/plain", content);

        assertTrue(storageService.exists(key));
        storageService.delete(key);
        assertFalse(storageService.exists(key));

        assertThrows(ResourceNotFoundException.class, () -> storageService.retrieve(key));
    }

    @Test
    @DisplayName("S3 Storage: Generate presigned download URL produces short-lived signed URL")
    void testGeneratePresignedUrl() {
        UUID orgId = UUID.randomUUID();
        byte[] content = "Signed URL test".getBytes(StandardCharsets.UTF_8);
        String key = storageService.store(orgId, "Invoice_2026.pdf", "application/pdf", content);

        String presignedUrl = storageService.generatePresignedDownloadUrl(key, "Invoice_2026.pdf", Duration.ofMinutes(15));

        assertNotNull(presignedUrl);
        assertTrue(presignedUrl.contains(key) || presignedUrl.contains("test-taxoryn-bucket"));
        // URL must contain signature parameter
        assertTrue(presignedUrl.contains("X-Amz-Signature") || presignedUrl.contains("X-Amz-Expires") || presignedUrl.contains("X-Amz-Credential"));
    }

    @Test
    @DisplayName("S3 Storage: Reject empty file payload on store")
    void testRejectEmptyFile() {
        UUID orgId = UUID.randomUUID();
        assertThrows(BadRequestException.class, () ->
                storageService.store(orgId, "empty.pdf", "application/pdf", new byte[0]));
    }

    @Test
    @DisplayName("S3 Storage: Reject path traversal in storage key")
    void testRejectPathTraversal() {
        assertThrows(BadRequestException.class, () ->
                storageService.retrieve("../../../etc/passwd"));
        assertThrows(BadRequestException.class, () ->
                storageService.retrieve("tenants/org_123/..\\..\\windows\\system32"));
    }
}
