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
                // Fail closed here rather than only relying on ProductionSecurityValidator (which
                // runs later, via SmartInitializingSingleton, after this bean would already have
                // been constructed). This bean is the actual point where LOCAL vs S3/R2 is chosen,
                // so it must never let production silently start on ephemeral local disk storage.
                String error = "CRITICAL SECURITY VIOLATION: Local filesystem storage ('taxoryn.storage.provider=LOCAL') "
                        + "is prohibited in production. Configure persistent S3/Cloudflare R2 storage "
                        + "(STORAGE_PROVIDER=S3, STORAGE_BUCKET, STORAGE_ACCESS_KEY, STORAGE_SECRET_KEY, "
                        + "and STORAGE_ENDPOINT or R2_ACCOUNT_ID).";
                log.error(error);
                throw new IllegalStateException(error);
            }
            log.info(">>> ACTIVE DOCUMENT STORAGE: LOCAL Filesystem Provider (base-dir='{}')",
                    storageProperties.getLocal().getBaseDir());
            LocalDocumentStorageService localService = new LocalDocumentStorageService(storageProperties);
            localService.init();
            return localService;
        }
    }
}
