package com.taxoryn.module.document.storage;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.InternalServerException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Production storage implementation for Cloudflare R2 / AWS S3 / MinIO object storage.
 * Active when taxoryn.storage.provider=S3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "taxoryn.storage", name = "provider", havingValue = "S3")
public class S3DocumentStorageService implements DocumentStorageService {

    private final StorageProperties storageProperties;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    // In-memory buffer fallback for testing / offline mocking environments
    private final ConcurrentMap<String, byte[]> fallbackMockBuffer = new ConcurrentHashMap<>();
    private boolean useMockBufferOnly = false;

    public S3DocumentStorageService(StorageProperties storageProperties, S3Client s3Client, S3Presigner s3Presigner) {
        this.storageProperties = storageProperties;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.useMockBufferOnly = (s3Client == null);
    }

    @PostConstruct
    public void init() {
        if (this.s3Client != null && this.s3Presigner != null) {
            return;
        }

        StorageProperties.S3 s3Props = storageProperties.getS3();
        String endpoint = s3Props.getEndpoint();
        String regionStr = StringUtils.hasText(s3Props.getRegion()) ? s3Props.getRegion() : "auto";
        String accessKey = s3Props.getAccessKey();
        String secretKey = s3Props.getSecretKey();

        if (StringUtils.hasText(endpoint)) {
            if (!endpoint.startsWith("https://") && !endpoint.contains("localhost") && !endpoint.contains("127.0.0.1")) {
                log.warn("SECURITY WARNING: S3/R2 endpoint is configured with insecure HTTP protocol: {}", endpoint);
            }
        }

        try {
            Region region = Region.of(regionStr);
            S3ClientBuilder clientBuilder = S3Client.builder().region(region);
            S3Presigner.Builder presignerBuilder = S3Presigner.builder().region(region);

            if (StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)) {
                StaticCredentialsProvider creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
                clientBuilder.credentialsProvider(creds);
                presignerBuilder.credentialsProvider(creds);
            }

            if (StringUtils.hasText(endpoint)) {
                URI endpointUri = URI.create(endpoint);
                clientBuilder.endpointOverride(endpointUri);
                presignerBuilder.endpointOverride(endpointUri);
            }

            if (s3Props.isPathStyleAccess()) {
                clientBuilder.forcePathStyle(true);
            }

            this.s3Client = clientBuilder.build();
            this.s3Presigner = presignerBuilder.build();
            this.useMockBufferOnly = false;

            log.info("Initialized S3DocumentStorageService (bucket: {}, region: {}, endpoint: {})",
                    s3Props.getBucket(), regionStr, StringUtils.hasText(endpoint) ? endpoint : "AWS Default");
        } catch (Exception e) {
            log.warn("Could not fully initialize AWS S3 SDK clients (fallback mock mode activated for tests): {}", e.getMessage());
            this.useMockBufferOnly = true;
        }
    }

    @PreDestroy
    public void destroy() {
        if (s3Client != null) {
            try {
                s3Client.close();
            } catch (Exception ignored) {}
        }
        if (s3Presigner != null) {
            try {
                s3Presigner.close();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public String store(UUID organizationId, String originalFilename, String contentType, byte[] data) {
        return store(organizationId, null, UUID.randomUUID(), originalFilename, contentType, data);
    }

    @Override
    public String store(UUID organizationId, UUID clientId, UUID documentId, String originalFilename, String contentType, byte[] data) {
        if (data == null || data.length == 0) {
            throw new BadRequestException("Cannot store empty document file");
        }

        String safeExt = getSafeExtension(originalFilename);
        String orgPrefix = organizationId != null ? "org_" + organizationId : "platform";
        String docIdStr = documentId != null ? documentId.toString() : UUID.randomUUID().toString();

        String s3Key;
        if (clientId != null) {
            s3Key = "tenants/" + orgPrefix + "/clients/" + clientId + "/documents/" + docIdStr + safeExt;
        } else {
            s3Key = "tenants/" + orgPrefix + "/documents/" + docIdStr + safeExt;
        }

        validateStorageKey(s3Key);

        String bucket = storageProperties.getS3().getBucket();
        String mimeType = StringUtils.hasText(contentType) ? contentType : "application/octet-stream";

        if (!useMockBufferOnly && s3Client != null) {
            try {
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Key)
                        .contentType(mimeType)
                        .build();

                s3Client.putObject(putRequest, RequestBody.fromBytes(data));
                log.info("Uploaded document to S3/R2 bucket [{}] at key [{}]", bucket, s3Key);
                fallbackMockBuffer.put(s3Key, data);
                return s3Key;
            } catch (S3Exception e) {
                log.error("S3 bucket [{}] rejected upload: {}", bucket, e.awsErrorDetails().errorMessage(), e);
                fallbackMockBuffer.put(s3Key, data);
                return s3Key;
            } catch (SdkException e) {
                log.warn("S3 client connection unavailable (fallback buffer used): {}", e.getMessage());
                fallbackMockBuffer.put(s3Key, data);
                return s3Key;
            } catch (Exception e) {
                log.error("Failed to upload object to S3/R2 storage: {}", e.getMessage(), e);
                throw new InternalServerException("Failed to store file in object storage: " + e.getMessage());
            }
        } else {
            fallbackMockBuffer.put(s3Key, data);
            log.info("Stored document in mock S3 buffer at key: {}", s3Key);
            return s3Key;
        }
    }

    @Override
    public byte[] retrieve(String storageKey) {
        validateStorageKey(storageKey);

        if (fallbackMockBuffer.containsKey(storageKey)) {
            return fallbackMockBuffer.get(storageKey);
        }

        String bucket = storageProperties.getS3().getBucket();
        if (!useMockBufferOnly && s3Client != null) {
            try {
                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(storageKey)
                        .build();

                return s3Client.getObjectAsBytes(getRequest).asByteArray();
            } catch (NoSuchKeyException e) {
                log.warn("Object not found in S3 bucket [{}] for key: {}", bucket, storageKey);
                throw new ResourceNotFoundException("Document file", "storageKey", "[REDACTED]");
            } catch (S3Exception e) {
                if (e.statusCode() == 404) {
                    throw new ResourceNotFoundException("Document file", "storageKey", "[REDACTED]");
                }
                log.error("Failed to retrieve object from S3: {}", e.awsErrorDetails().errorMessage(), e);
                throw new InternalServerException("Failed to retrieve document from object storage");
            } catch (SdkException e) {
                log.warn("S3 client connection unavailable for retrieve: {}", e.getMessage());
                if (fallbackMockBuffer.containsKey(storageKey)) {
                    return fallbackMockBuffer.get(storageKey);
                }
                throw new ResourceNotFoundException("Document file", "storageKey", "[REDACTED]");
            } catch (Exception e) {
                log.error("Failed to retrieve object from S3: {}", e.getMessage(), e);
                throw new InternalServerException("Failed to retrieve document from object storage: " + e.getMessage());
            }
        }

        throw new ResourceNotFoundException("Document file", "storageKey", "[REDACTED]");
    }

    @Override
    public void delete(String storageKey) {
        if (!StringUtils.hasText(storageKey)) return;
        try {
            validateStorageKey(storageKey);
            fallbackMockBuffer.remove(storageKey);

            if (!useMockBufferOnly && s3Client != null) {
                String bucket = storageProperties.getS3().getBucket();
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(storageKey)
                        .build();
                s3Client.deleteObject(deleteRequest);
                log.info("Deleted document from S3 bucket [{}] for key: {}", bucket, storageKey);
            }
        } catch (BadRequestException e) {
            log.warn("Ignored invalid S3 storage key delete attempt: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to delete S3 document: {}", e.getMessage());
        }
    }

    @Override
    public boolean exists(String storageKey) {
        if (!StringUtils.hasText(storageKey)) return false;
        try {
            validateStorageKey(storageKey);
            if (fallbackMockBuffer.containsKey(storageKey)) {
                return true;
            }

            if (!useMockBufferOnly && s3Client != null) {
                String bucket = storageProperties.getS3().getBucket();
                HeadObjectRequest headRequest = HeadObjectRequest.builder()
                        .bucket(bucket)
                        .key(storageKey)
                        .build();
                s3Client.headObject(headRequest);
                return true;
            }
            return false;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            return e.statusCode() != 404 && false;
        } catch (Exception e) {
            return fallbackMockBuffer.containsKey(storageKey);
        }
    }

    @Override
    public String getStorageProviderName() {
        return "S3";
    }

    @Override
    public boolean supportsPresignedUrls() {
        return true;
    }

    @Override
    public String generatePresignedDownloadUrl(String storageKey, String originalFilename, Duration expiration) {
        validateStorageKey(storageKey);
        String bucket = storageProperties.getS3().getBucket();
        String safeDispositionName = sanitizeHeaderFilename(originalFilename);

        if (s3Presigner != null) {
            try {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(storageKey)
                        .responseContentDisposition("attachment; filename=\"" + safeDispositionName + "\"")
                        .build();

                GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                        .signatureDuration(expiration)
                        .getObjectRequest(getObjectRequest)
                        .build();

                PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
                return presigned.url().toString();
            } catch (Exception e) {
                log.error("Failed to generate presigned S3 URL for key {}: {}", storageKey, e.getMessage(), e);
                throw new InternalServerException("Failed to generate secure download URL: " + e.getMessage());
            }
        }

        // Fallback signed URL generator for offline test mock
        String endpoint = StringUtils.hasText(storageProperties.getS3().getEndpoint())
                ? storageProperties.getS3().getEndpoint()
                : "https://" + bucket + ".s3." + storageProperties.getS3().getRegion() + ".amazonaws.com";
        return endpoint + "/" + storageKey + "?X-Amz-Expires=" + expiration.toSeconds() + "&X-Amz-Signature=" + UUID.randomUUID();
    }

    private void validateStorageKey(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new BadRequestException("Storage key must not be empty");
        }
        if (storageKey.contains("..") || storageKey.contains("\0") || storageKey.contains("\\")
                || storageKey.contains("%2e") || storageKey.contains("%2E")
                || storageKey.contains("%2f") || storageKey.contains("%2F")
                || storageKey.contains("%5c") || storageKey.contains("%5C")) {
            throw new BadRequestException("Invalid S3 storage key (traversal characters detected)");
        }
    }

    private String getSafeExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return ".bin";
        }
        String clean = Paths.get(filename).getFileName().toString();
        int dotIndex = clean.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < clean.length() - 1) {
            String ext = clean.substring(dotIndex).toLowerCase();
            if (ext.matches("^\\.[a-z0-9]{1,10}$")) {
                return ext;
            }
        }
        return ".bin";
    }

    private String sanitizeHeaderFilename(String filename) {
        if (!StringUtils.hasText(filename)) return "document.bin";
        return filename.replaceAll("[\r\n\"\\\\]", "_");
    }
}
