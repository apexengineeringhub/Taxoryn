package com.taxoryn.module.document.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Dynamic configuration and lifecycle management for document storage.
 * Automatically chooses S3/R2 storage when configured or credentials/endpoint are detected,
 * validates required properties, and fails closed in production if storage is misconfigured.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DocumentStorageConfig {

    private final StorageProperties storageProperties;
    private final Environment environment;

    @Bean
    @Primary
    public DocumentStorageService documentStorageService() {
        boolean isProd = environment.acceptsProfiles(Profiles.of("prod"));

        if (storageProperties.isS3Provider()) {
            storageProperties.validateS3Configuration(isProd);

            String endpoint = storageProperties.getS3().getResolvedEndpoint();
            log.info(">>> ACTIVE DOCUMENT STORAGE: S3/R2 Provider (bucket='{}', region='{}', endpoint='{}')",
                    storageProperties.getS3().getBucket(),
                    storageProperties.getS3().getRegion(),
                    endpoint != null ? endpoint : "AWS Default");

            S3DocumentStorageService s3Service = new S3DocumentStorageService(storageProperties);
            s3Service.init();
            return s3Service;
        } else {
            if (isProd) {
                log.warn(">>> PRODUCTION WARNING: Storage provider is set to LOCAL filesystem storage at '{}'. In multi-instance / container deployments, configure STORAGE_PROVIDER=S3 with Cloudflare R2 / AWS S3 for persistent storage.",
                        storageProperties.getLocal().getBaseDir());
            } else {
                log.info(">>> ACTIVE DOCUMENT STORAGE: LOCAL Filesystem Provider (base-dir='{}')",
                        storageProperties.getLocal().getBaseDir());
            }
            LocalDocumentStorageService localService = new LocalDocumentStorageService(storageProperties);
            localService.init();
            return localService;
        }
    }
}
