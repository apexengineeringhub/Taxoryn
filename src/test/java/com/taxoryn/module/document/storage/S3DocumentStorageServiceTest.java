package com.taxoryn.module.document.storage;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.InternalServerException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3DocumentStorageServiceTest {

    private StorageProperties storageProperties;
    private S3Client mockS3Client;
    private S3Presigner mockS3Presigner;
    private S3DocumentStorageService storageService;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setProvider("S3");
        storageProperties.setPresignedUrlDurationMinutes(15);
        storageProperties.setMaxPresignedUrlDurationMinutes(60);
        storageProperties.getS3().setBucket("taxoryn-documents");
        storageProperties.getS3().setRegion("auto");
        storageProperties.getS3().setAccessKey("test-access-key");
        storageProperties.getS3().setSecretKey("test-secret-key-12345");
        storageProperties.getS3().setEndpoint("https://test-account.r2.cloudflarestorage.com");

        mockS3Client = mock(S3Client.class);
        mockS3Presigner = mock(S3Presigner.class);

        storageService = new S3DocumentStorageService(storageProperties, mockS3Client, mockS3Presigner);
    }

    @Test
    @DisplayName("S3 Storage: Provider metadata returns S3 and supports presigned URLs")
    void testStorageProviderMetadata() {
        assertEquals("S3", storageService.getStorageProviderName());
        assertTrue(storageService.supportsPresignedUrls());
    }

    @Test
    @DisplayName("S3 Storage: Store document executes PutObject followed by HeadObject verification")
    void testStoreAndVerifyStructuredDocument() {
        UUID orgId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        byte[] content = "Audit Report PDF Data".getBytes(StandardCharsets.UTF_8);

        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        when(mockS3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        String key = storageService.store(orgId, clientId, docId, "AuditReport.pdf", "application/pdf", content);

        assertNotNull(key);
        assertTrue(key.startsWith("tenants/org_" + orgId + "/clients/" + clientId + "/documents/" + docId));
        assertTrue(key.endsWith(".pdf"));

        verify(mockS3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(mockS3Client, times(1)).headObject(any(HeadObjectRequest.class));
    }

    @Test
    @DisplayName("S3 Storage: Store document fails and cleans up partial object when HeadObject verification fails")
    void testStoreDocumentVerificationFailureTriggersCleanup() {
        UUID orgId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        byte[] content = "Content".getBytes(StandardCharsets.UTF_8);

        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        when(mockS3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Object not found after put").build());

        InternalServerException ex = assertThrows(InternalServerException.class, () ->
                storageService.store(orgId, null, docId, "Policy.pdf", "application/pdf", content));

        assertTrue(ex.getMessage().contains("Document storage verification failed"));
        // Verify cleanup was attempted
        verify(mockS3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("S3 Storage: Store document fails when PutObject throws S3Exception")
    void testStoreDocumentPutObjectFailure() {
        UUID orgId = UUID.randomUUID();
        byte[] content = "Content".getBytes(StandardCharsets.UTF_8);

        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().statusCode(403).message("Access Denied").build());

        InternalServerException ex = assertThrows(InternalServerException.class, () ->
                storageService.store(orgId, "document.pdf", "application/pdf", content));

        assertTrue(ex.getMessage().contains("rejected upload"));
        verify(mockS3Client, never()).headObject(any(HeadObjectRequest.class));
    }

    @Test
    @DisplayName("S3 Storage: Retrieve returns real bytes from S3Client")
    void testRetrieveDocumentSuccess() {
        String key = "tenants/org_123/documents/doc_456.pdf";
        byte[] content = "PDF Payload Content".getBytes(StandardCharsets.UTF_8);

        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(), content);

        when(mockS3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        byte[] retrieved = storageService.retrieve(key);
        assertArrayEquals(content, retrieved);
        verify(mockS3Client, times(1)).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("S3 Storage: Retrieve maps NoSuchKeyException to ResourceNotFoundException")
    void testRetrieveDocumentNotFound() {
        String key = "tenants/org_123/documents/missing.pdf";

        when(mockS3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("No such key").build());

        assertThrows(ResourceNotFoundException.class, () -> storageService.retrieve(key));
    }

    @Test
    @DisplayName("S3 Storage: Delete removes object and handles NoSuchKeyException idempotently")
    void testDeleteDocumentIdempotent() {
        String key = "tenants/org_123/documents/doc_456.pdf";

        when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Already deleted").build());

        assertDoesNotThrow(() -> storageService.delete(key));
        verify(mockS3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("S3 Storage: Exists returns true for existing object and false for 404")
    void testExistsCheck() {
        String existingKey = "tenants/org_123/documents/existing.pdf";
        String missingKey = "tenants/org_123/documents/missing.pdf";

        when(mockS3Client.headObject(argThat((HeadObjectRequest r) -> r != null && existingKey.equals(r.key()))))
                .thenReturn(HeadObjectResponse.builder().build());
        when(mockS3Client.headObject(argThat((HeadObjectRequest r) -> r != null && missingKey.equals(r.key()))))
                .thenThrow(NoSuchKeyException.builder().message("Not found").build());

        assertTrue(storageService.exists(existingKey));
        assertFalse(storageService.exists(missingKey));
    }

    @Test
    @DisplayName("S3 Storage: Exists throws InternalServerException on 500/network errors without hiding error")
    void testExistsThrowsOnServerError() {
        String key = "tenants/org_123/documents/error.pdf";

        when(mockS3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("Internal S3 Error").build());

        InternalServerException ex = assertThrows(InternalServerException.class, () ->
                storageService.exists(key));
        assertTrue(ex.getMessage().contains("Failed to check document existence"));
    }

    @Test
    @DisplayName("S3 Storage: Generate presigned download URL invokes S3Presigner with bounded expiration")
    void testGeneratePresignedUrl() throws Exception {
        String key = "tenants/org_123/documents/Form16.pdf";
        URL fakePresignedUrl = URI.create("https://test-account.r2.cloudflarestorage.com/taxoryn-documents/Form16.pdf?X-Amz-Signature=sig123&X-Amz-Expires=900").toURL();

        PresignedGetObjectRequest mockPresignedRequest = mock(PresignedGetObjectRequest.class);
        when(mockPresignedRequest.url()).thenReturn(fakePresignedUrl);
        when(mockS3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresignedRequest);

        String presignedUrl = storageService.generatePresignedDownloadUrl(key, "Form16.pdf", Duration.ofMinutes(15));

        assertNotNull(presignedUrl);
        assertTrue(presignedUrl.contains("X-Amz-Signature=sig123"));
        verify(mockS3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
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

    @Test
    @DisplayName("S3 Configuration: Validate rejects missing required properties and insecure HTTP in production")
    void testConfigurationValidation() {
        StorageProperties props = new StorageProperties();
        props.setProvider("S3");
        props.getS3().setBucket("");
        props.getS3().setAccessKey("");
        props.getS3().setSecretKey("");

        assertThrows(IllegalStateException.class, () -> props.validateS3Configuration(true));

        props.getS3().setBucket("valid-bucket");
        props.getS3().setAccessKey("valid-key");
        props.getS3().setSecretKey("valid-secret");
        props.getS3().setEndpoint("http://insecure-r2-endpoint.com");

        // Insecure HTTP rejected in production profile
        assertThrows(IllegalStateException.class, () -> props.validateS3Configuration(true));

        // Insecure HTTP allowed on localhost for local dev
        props.getS3().setEndpoint("http://localhost:9000");
        assertDoesNotThrow(() -> props.validateS3Configuration(true));

        // HTTPS accepted
        props.getS3().setEndpoint("https://account123.r2.cloudflarestorage.com");
        assertDoesNotThrow(() -> props.validateS3Configuration(true));
    }
}
