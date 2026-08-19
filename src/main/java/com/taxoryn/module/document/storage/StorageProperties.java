package com.taxoryn.module.document.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "taxoryn.storage")
public class StorageProperties {

    /**
     * Active storage provider: LOCAL or S3
     */
    private String provider = "LOCAL";

    private Local local = new Local();
    private S3 s3 = new S3();

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
        private String region = "ap-south-1";
        private String accessKey;
        private String secretKey;
        private String endpoint;
        private boolean pathStyleAccess = false;
    }
}
