package com.taxoryn.module.document.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.net.URL;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductionStorageConfigurationSecurityTest {

    // =========================================================================
    // 1. DocumentStorageConfig Fail-Closed Tests in Production
    // =========================================================================

    @Test
    @DisplayName("Fail-Closed: DocumentStorageConfig rejects LOCAL storage provider in production profile")
    void testDocumentStorageConfigRejectsLocalInProduction() {
        Environment env = mock(Environment.class);
        when(env.acceptsProfiles(any(Profiles.class))).thenReturn(true); // prod profile active

        StorageProperties properties = new StorageProperties();
        properties.setProvider("LOCAL");

        DocumentStorageConfig config = new DocumentStorageConfig(properties, env);

        IllegalStateException ex = assertThrows(IllegalStateException.class, config::documentStorageService);
        assertTrue(ex.getMessage().contains("Local filesystem storage ('taxoryn.storage.provider=LOCAL') is prohibited in production"));
    }

    @Test
    @DisplayName("Success: DocumentStorageConfig boots S3DocumentStorageService when S3/R2 is configured in production")
    void testDocumentStorageConfigConstructsS3ServiceInProduction() {
        Environment env = mock(Environment.class);
        when(env.acceptsProfiles(any(Profiles.class))).thenReturn(true); // prod profile active

        StorageProperties properties = new StorageProperties();
        properties.setProvider("S3");
        properties.getS3().setBucket("taxoryn-production-documents");
        properties.getS3().setRegion("auto");
        properties.getS3().setAccessKey("AKIAIOSFODNN7EXAMPLE");
        properties.getS3().setSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        properties.getS3().setEndpoint("https://test-account-id.r2.cloudflarestorage.com");

        DocumentStorageConfig config = new DocumentStorageConfig(properties, env);

        DocumentStorageService service = config.documentStorageService();
        assertNotNull(service);
        assertTrue(service instanceof S3DocumentStorageService);
        assertEquals("S3", service.getStorageProviderName());
        assertTrue(service.supportsPresignedUrls());
    }

    @Test
    @DisplayName("Success: DocumentStorageConfig allows LOCAL storage provider in development profile")
    void testDocumentStorageConfigAllowsLocalInDevelopment() {
        Environment env = mock(Environment.class);
        when(env.acceptsProfiles(any(Profiles.class))).thenReturn(false); // dev profile (not prod)

        StorageProperties properties = new StorageProperties();
        properties.setProvider("LOCAL");
        properties.getLocal().setBaseDir("./target/test-dev-docs");

        DocumentStorageConfig config = new DocumentStorageConfig(properties, env);

        DocumentStorageService service = config.documentStorageService();
        assertNotNull(service);
        assertTrue(service instanceof LocalDocumentStorageService);
        assertEquals("LOCAL", service.getStorageProviderName());
        assertFalse(service.supportsPresignedUrls());
    }

    // =========================================================================
    // 2. StorageProperties Configuration Validation Tests
    // =========================================================================

    @Test
    @DisplayName("Fail-Fast: StorageProperties rejects missing S3 bucket")
    void testValidateS3ConfigMissingBucket() {
        StorageProperties properties = new StorageProperties();
        properties.setProvider("S3");
        properties.getS3().setBucket("");
        properties.getS3().setAccessKey("valid-access-key");
        properties.getS3().setSecretKey("valid-secret-key");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> properties.validateS3Configuration(true));
        assertTrue(ex.getMessage().contains("S3/R2 bucket name is required"));
    }

    @Test
    @DisplayName("Fail-Fast: StorageProperties rejects missing S3 access key")
    void testValidateS3ConfigMissingAccessKey() {
        StorageProperties properties = new StorageProperties();
        properties.setProvider("S3");
        properties.getS3().setBucket("valid-bucket");
        properties.getS3().setAccessKey("");
        properties.getS3().setSecretKey("valid-secret-key");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> properties.validateS3Configuration(true));
        assertTrue(ex.getMessage().contains("S3/R2 access key is required"));
    }

    @Test
    @DisplayName("Fail-Fast: StorageProperties rejects missing S3 secret key")
    void testValidateS3ConfigMissingSecretKey() {
        StorageProperties properties = new StorageProperties();
        properties.setProvider("S3");
        properties.getS3().setBucket("valid-bucket");
        properties.getS3().setAccessKey("valid-access-key");
        properties.getS3().setSecretKey("");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> properties.validateS3Configuration(true));
        assertTrue(ex.getMessage().contains("S3/R2 secret key is required"));
    }

    @Test
    @DisplayName("Fail-Fast: StorageProperties rejects insecure HTTP endpoint in production")
    void testValidateS3ConfigInsecureHttpInProduction() {
        StorageProperties properties = new StorageProperties();
        properties.setProvider("S3");
        properties.getS3().setBucket("valid-bucket");
        properties.getS3().setAccessKey("valid-access-key");
        properties.getS3().setSecretKey("valid-secret-key");
        properties.getS3().setEndpoint("http://remote-s3.example.com");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> properties.validateS3Configuration(true));
        assertTrue(ex.getMessage().contains("Insecure HTTP S3/R2 endpoint rejected in production"));
    }

    @Test
    @DisplayName("Success: StorageProperties allows localhost HTTP endpoint in dev/test")
    void testValidateS3ConfigLocalhostHttpAllowed() {
        StorageProperties properties = new StorageProperties();
        properties.setProvider("S3");
        properties.getS3().setBucket("valid-bucket");
        properties.getS3().setAccessKey("valid-access-key");
        properties.getS3().setSecretKey("valid-secret-key");
        properties.getS3().setEndpoint("http://localhost:9000");

        assertDoesNotThrow(() -> properties.validateS3Configuration(true));
    }

    @Test
    @DisplayName("Success: StorageProperties resolves Cloudflare R2 endpoint from accountId")
    void testValidateS3ConfigResolvesEndpointFromAccountId() {
        StorageProperties properties = new StorageProperties();
        properties.setProvider("R2");
        properties.getS3().setBucket("taxoryn-r2-bucket");
        properties.getS3().setAccessKey("valid-access-key");
        properties.getS3().setSecretKey("valid-secret-key");
        properties.getS3().setAccountId("0123456789abcdef0123456789abcdef");

        assertEquals("https://0123456789abcdef0123456789abcdef.r2.cloudflarestorage.com",
                properties.getS3().getResolvedEndpoint());
        assertDoesNotThrow(() -> properties.validateS3Configuration(true));
    }

    // =========================================================================
    // 3. Security & Actuator Health Privacy Tests
    // =========================================================================

    @Test
    @DisplayName("Security: S3StorageHealthIndicator exposes bucket, region, and endpoint without leaking credentials")
    void testStorageHealthIndicatorExposesNoSecrets() {
        StorageProperties properties = new StorageProperties();
        properties.setProvider("S3");
        properties.getS3().setBucket("taxoryn-production-docs");
        properties.getS3().setRegion("auto");
        properties.getS3().setAccessKey("SECRET_AKIA_KEY_NEVER_LOG");
        properties.getS3().setSecretKey("SECRET_AWS_KEY_NEVER_LOG");
        properties.getS3().setEndpoint("https://account123.r2.cloudflarestorage.com");

        DocumentStorageService mockService = mock(DocumentStorageService.class);
        when(mockService.getStorageProviderName()).thenReturn("S3");
        when(mockService.supportsPresignedUrls()).thenReturn(true);

        S3StorageHealthIndicator healthIndicator = new S3StorageHealthIndicator(properties, mockService);
        Health health = healthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("S3/R2", health.getDetails().get("provider"));
        assertEquals("taxoryn-production-docs", health.getDetails().get("bucket"));
        assertEquals("auto", health.getDetails().get("region"));
        assertEquals("https://account123.r2.cloudflarestorage.com", health.getDetails().get("endpoint"));
        assertEquals(true, health.getDetails().get("presignedUrlSupported"));

        // Verify credentials are not in health details
        assertNull(health.getDetails().get("accessKey"));
        assertNull(health.getDetails().get("secretKey"));
        assertFalse(health.getDetails().toString().contains("SECRET_AKIA_KEY_NEVER_LOG"));
        assertFalse(health.getDetails().toString().contains("SECRET_AWS_KEY_NEVER_LOG"));
    }

    // =========================================================================
    // 4. Presigned URL Bounds & Generation Tests
    // =========================================================================

    @Test
    @DisplayName("Presigned URLs: Generation enforces maxPresignedUrlDurationMinutes bounds")
    void testGeneratePresignedUrlEnforcesDurationBounds() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setProvider("S3");
        properties.setPresignedUrlDurationMinutes(15);
        properties.setMaxPresignedUrlDurationMinutes(60);
        properties.getS3().setBucket("taxoryn-documents");
        properties.getS3().setRegion("auto");
        properties.getS3().setAccessKey("valid-key");
        properties.getS3().setSecretKey("valid-secret");

        S3Presigner mockPresigner = mock(S3Presigner.class);
        PresignedGetObjectRequest mockPresigned = mock(PresignedGetObjectRequest.class);
        URL presignedUrl = URI.create("https://account123.r2.cloudflarestorage.com/taxoryn-documents/tenants/org_1/documents/doc1.pdf?X-Amz-Signature=sig123").toURL();
        when(mockPresigned.url()).thenReturn(presignedUrl);
        when(mockPresigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresigned);

        S3DocumentStorageService storageService = new S3DocumentStorageService(properties, mock(software.amazon.awssdk.services.s3.S3Client.class), mockPresigner);

        String url = storageService.generatePresignedDownloadUrl("tenants/org_1/documents/doc1.pdf", "doc1.pdf", Duration.ofMinutes(120));
        assertNotNull(url);
        assertTrue(url.contains("X-Amz-Signature"));

        verify(mockPresigner, times(1)).presignGetObject(argThat((GetObjectPresignRequest req) ->
                req != null && req.signatureDuration().toMinutes() == 60 // capped at max 60 minutes
        ));
    }
}
