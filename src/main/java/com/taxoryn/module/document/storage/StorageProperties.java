package com.taxoryn.module.document.storage;

import com.taxoryn.core.exception.BadRequestException;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.URI;

@Data
@Configuration
@ConfigurationProperties(prefix = "taxoryn.storage")
public class StorageProperties {

    /**
     * Active storage provider: LOCAL or S3 / R2 / CLOUDFLARE_R2 / MINIO / AWS
     */
    private String provider = "LOCAL";

    /**
     * Default duration in minutes for signed download URLs (default 15 minutes).
     */
    private int presignedUrlDurationMinutes = 15;

    /**
     * Maximum allowed duration in minutes for signed download URLs (default 60 minutes).
     */
    private int maxPresignedUrlDurationMinutes = 60;

    private Local local = new Local();
    private S3 s3 = new S3();

    /**
     * Returns true if S3/R2 storage is active or should be auto-detected.
     */
    public boolean isS3Provider() {
        if (StringUtils.hasText(provider)) {
            String p = provider.trim().toUpperCase();
            if (p.equals("S3") || p.equals("R2") || p.equals("CLOUDFLARE") || p.equals("CLOUDFLARE_R2")
                    || p.equals("AWS") || p.equals("AWS_S3") || p.equals("MINIO")) {
                return true;
            }
            if (p.equals("LOCAL")) {
                if (s3 != null && StringUtils.hasText(s3.getAccessKey()) && StringUtils.hasText(s3.getSecretKey())
                        && (StringUtils.hasText(s3.getEndpoint()) || StringUtils.hasText(s3.getAccountId()))) {
                    return true;
                }
                return false;
            }
        }
        if (s3 != null && StringUtils.hasText(s3.getAccessKey()) && StringUtils.hasText(s3.getSecretKey())) {
            return true;
        }
        return false;
    }

    /**
     * Validates S3/R2 properties when S3 provider is active.
     */
    public void validateS3Configuration(boolean isProductionProfile) {
        if (!isS3Provider()) {
            return;
        }
        if (s3 == null) {
            throw new IllegalStateException("S3 configuration block ('taxoryn.storage.s3') is missing");
        }
        if (!StringUtils.hasText(s3.getBucket())) {
            throw new IllegalStateException("S3/R2 bucket name is required ('taxoryn.storage.s3.bucket' / STORAGE_BUCKET)");
        }
        if (!StringUtils.hasText(s3.getAccessKey())) {
            throw new IllegalStateException("S3/R2 access key is required ('taxoryn.storage.s3.access-key' / STORAGE_ACCESS_KEY)");
        }
        if (!StringUtils.hasText(s3.getSecretKey())) {
            throw new IllegalStateException("S3/R2 secret key is required ('taxoryn.storage.s3.secret-key' / STORAGE_SECRET_KEY)");
        }

        String endpoint = s3.getResolvedEndpoint();
        if (StringUtils.hasText(endpoint)) {
            try {
                URI uri = URI.create(endpoint);
                String scheme = uri.getScheme();
                if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                    throw new IllegalStateException("S3/R2 endpoint must use http:// or https:// scheme: " + endpoint);
                }
                String host = uri.getHost();
                boolean isLocalhost = host != null && (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1"));
                if ("http".equalsIgnoreCase(scheme) && !isLocalhost && isProductionProfile) {
                    throw new IllegalStateException("Insecure HTTP S3/R2 endpoint rejected in production: " + endpoint);
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Invalid S3/R2 endpoint URI: " + endpoint, e);
            }
        }
    }

    @Data
    public static class Local {
        /**
         * Base directory on filesystem for document uploads.
         */
        private String baseDir = "./data/documents";
    }

    @Data
    public static class S3 {
        private String bucket = "taxoryn-documents";
        private String region = "auto";
        private String accessKey;
        private String secretKey;
        private String endpoint;
        private String accountId;
        private boolean pathStyleAccess = true;
        private boolean chunkedEncodingEnabled = false;

        public String getResolvedEndpoint() {
            if (StringUtils.hasText(endpoint)) {
                return endpoint.trim();
            }
            if (StringUtils.hasText(accountId)) {
                return "https://" + accountId.trim() + ".r2.cloudflarestorage.com";
            }
            return null;
        }
    }
}

