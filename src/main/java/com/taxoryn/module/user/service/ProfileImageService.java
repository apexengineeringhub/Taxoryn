package com.taxoryn.module.user.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Service interface for validating, storing, and resolving user and employee profile avatars.
 */
public interface ProfileImageService {

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    class AvatarContent {
        private String contentType;
        private byte[] data;
        private long fileSize;
    }

    void validateImage(MultipartFile file);

    void scanForMalware(byte[] bytes, String originalFilename);

    String storeAvatar(UUID organizationId, UUID entityId, String entityType, MultipartFile file, String previousStorageKey);

    void deleteAvatar(String storageKey);

    String resolveAvatarUrl(String avatarKeyOrUrl);

    byte[] retrieveAvatarContent(String storageKey);

    AvatarContent retrieveAvatar(String storageKey);

    String detectImageContentType(byte[] header, String storageKey);
}
