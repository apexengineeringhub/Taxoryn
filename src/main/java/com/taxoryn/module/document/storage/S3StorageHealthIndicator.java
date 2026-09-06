package com.taxoryn.module.document.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;

/**
 * Spring Boot Actuator health indicator for Taxoryn document storage.
 * Safe and production-ready: never exposes credentials or sensitive secrets.
 */
@Slf4j
@Component("storageHealthIndicator")
@RequiredArgsConstructor
public class S3StorageHealthIndicator implements HealthIndicator {

    private final StorageProperties storageProperties;
    private final DocumentStorageService documentStorageService;

    @Override
    public Health health() {
        try {
            String provider = documentStorageService.getStorageProviderName();

            if ("S3".equalsIgnoreCase(provider)) {
                StorageProperties.S3 s3Props = storageProperties.getS3();
                String bucket = s3Props != null ? s3Props.getBucket() : "unknown";
                String region = s3Props != null ? s3Props.getRegion() : "auto";
                String endpoint = s3Props != null ? s3Props.getResolvedEndpoint() : null;

                return Health.up()
                        .withDetail("provider", "S3/R2")
                        .withDetail("bucket", bucket)
                        .withDetail("region", region)
                        .withDetail("endpoint", StringUtils.hasText(endpoint) ? endpoint : "AWS Default")
                        .withDetail("presignedUrlSupported", documentStorageService.supportsPresignedUrls())
                        .build();
            } else {
                String baseDir = storageProperties.getLocal().getBaseDir();
                File dir = new File(baseDir);
                boolean existsAndWritable = dir.exists() && dir.canWrite();

                if (existsAndWritable) {
                    return Health.up()
                            .withDetail("provider", "LOCAL")
                            .withDetail("baseDir", baseDir)
                            .withDetail("writable", true)
                            .build();
                } else {
                    return Health.down()
                            .withDetail("provider", "LOCAL")
                            .withDetail("baseDir", baseDir)
                            .withDetail("error", "Directory does not exist or is not writable")
                            .build();
                }
            }
        } catch (Exception e) {
            log.warn("Storage health check failed: {}", e.getMessage());
            return Health.down(e)
                    .withDetail("error", "Storage health verification failed")
                    .build();
        }
    }
}
