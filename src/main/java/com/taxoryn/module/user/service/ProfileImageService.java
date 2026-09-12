package com.taxoryn.module.user.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.InternalServerException;
import com.taxoryn.core.security.upload.MalwareScanner;
import com.taxoryn.module.document.storage.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final DocumentStorageService documentStorageService;
    private final MalwareScanner malwareScanner;

    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file cannot be empty");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BadRequestException("Image file size exceeds maximum limit of 5MB");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase().trim())) {
            throw new BadRequestException("Invalid image type '" + contentType + "'. Supported formats: PNG, JPEG, WEBP, GIF");
        }

        try {
            byte[] header = new byte[Math.min(16, (int) file.getSize())];
            try (var is = file.getInputStream()) {
                int read = is.read(header);
                if (read < 3 || !isImageMagicBytesValid(header, read)) {
                    throw new BadRequestException("Corrupted or unsupported image file header");
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read image content: " + e.getMessage());
        }
    }

    private boolean isImageMagicBytesValid(byte[] header, int length) {
        if (length < 3) return false;

        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return true;
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (length >= 8
                && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
            return true;
        }

        // GIF: GIF87a or GIF89a (0x47 0x49 0x46 0x38)
        if (length >= 6 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8') {
            return true;
        }

        // WEBP: RIFF....WEBP (0x52 0x49 0x46 0x46 ... 0x57 0x45 0x42 0x50)
        if (length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return true;
        }

        return false;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AvatarContent {
        private String contentType;
        private byte[] data;
        private long fileSize;
    }

    public void scanForMalware(byte[] bytes, String originalFilename) {
        if (malwareScanner != null) {
            try {
                var scanResult = malwareScanner.scan(bytes, StringUtils.hasText(originalFilename) ? originalFilename : "avatar.png");
                if (scanResult.isInfected()) {
                    log.warn("SECURITY ALERT: Malware detected in profile photo upload: {}", scanResult.getThreatName());
                    throw new BadRequestException("File failed security scanning and cannot be accepted");
                }
                if (scanResult.isFailed()) {
                    log.error("Malware scanner returned fail status for profile photo: {}", scanResult.getDetails());
                    throw new BadRequestException("File scanning service encountered an error. Upload rejected (fail-closed).");
                }
            } catch (BadRequestException bre) {
                throw bre;
            } catch (Exception e) {
                log.error("Fail-closed: Malware scanner threw error during profile photo inspection: {}", e.getMessage(), e);
                throw new BadRequestException("File scanning verification failed (fail-closed).");
            }
        }
    }

    public String storeAvatar(UUID organizationId, UUID entityId, String entityType, MultipartFile file, String previousStorageKey) {
        validateImage(file);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new InternalServerException("Failed to read upload payload: " + e.getMessage());
        }

        scanForMalware(bytes, file.getOriginalFilename());

        String safeExt = getSafeImageExtension(file.getOriginalFilename(), file.getContentType());
        String orgPrefix = organizationId != null ? "org_" + organizationId : "platform";
        String storageKey = "tenants/" + orgPrefix + "/avatars/" + entityType + "_" + entityId + "_" + UUID.randomUUID() + safeExt;

        String storedKey = documentStorageService.store(organizationId, null, entityId, file.getOriginalFilename(), file.getContentType(), bytes);

        // If old avatar was stored in storage backend, safely delete it after successful new store
        if (StringUtils.hasText(previousStorageKey) && previousStorageKey.startsWith("tenants/") && !previousStorageKey.equals(storedKey)) {
            try {
                documentStorageService.delete(previousStorageKey);
                log.debug("Cleaned up previous avatar object: {}", previousStorageKey);
            } catch (Exception ex) {
                log.warn("Failed to delete orphaned previous avatar [{}]: {}", previousStorageKey, ex.getMessage());
            }
        }

        return storedKey;
    }

    public void deleteAvatar(String storageKey) {
        if (StringUtils.hasText(storageKey) && storageKey.startsWith("tenants/")) {
            try {
                documentStorageService.delete(storageKey);
            } catch (Exception e) {
                log.warn("Failed to delete avatar from storage [{}]: {}", storageKey, e.getMessage());
            }
        }
    }

    public String resolveAvatarUrl(String avatarKeyOrUrl) {
        if (!StringUtils.hasText(avatarKeyOrUrl)) {
            return null;
        }

        if (avatarKeyOrUrl.startsWith("http://") || avatarKeyOrUrl.startsWith("https://") || avatarKeyOrUrl.startsWith("data:")) {
            return avatarKeyOrUrl;
        }

        if (documentStorageService.supportsPresignedUrls()) {
            try {
                return documentStorageService.generatePresignedDownloadUrl(avatarKeyOrUrl, "avatar.png", Duration.ofMinutes(15));
            } catch (Exception ex) {
                log.warn("Failed to generate presigned avatar URL for key [{}]: {}", avatarKeyOrUrl, ex.getMessage());
            }
        }

        return avatarKeyOrUrl;
    }

    public byte[] retrieveAvatarContent(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new BadRequestException("Storage key is required");
        }
        return documentStorageService.retrieve(storageKey);
    }

    public AvatarContent retrieveAvatar(String storageKey) {
        byte[] bytes = retrieveAvatarContent(storageKey);
        String contentType = detectImageContentType(bytes, storageKey);
        return AvatarContent.builder()
                .data(bytes)
                .contentType(contentType)
                .fileSize(bytes != null ? bytes.length : 0L)
                .build();
    }

    public String detectImageContentType(byte[] header, String storageKey) {
        if (header != null && header.length >= 3) {
            // JPEG: FF D8 FF
            if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }
            // PNG: 89 50 4E 47 0D 0A 1A 0A
            if (header.length >= 8
                    && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                    && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
                return "image/png";
            }
            // GIF: GIF87a or GIF89a (0x47 0x49 0x46 0x38)
            if (header.length >= 6 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8') {
                return "image/gif";
            }
            // WEBP: RIFF....WEBP (0x52 0x49 0x46 0x46 ... 0x57 0x45 0x42 0x50)
            if (header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                return "image/webp";
            }
        }
        if (StringUtils.hasText(storageKey)) {
            String lower = storageKey.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".webp")) return "image/webp";
            if (lower.endsWith(".gif")) return "image/gif";
        }
        return "image/png";
    }

    private String getSafeImageExtension(String filename, String contentType) {
        if (StringUtils.hasText(filename)) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".png")) return ".png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return ".jpg";
            if (lower.endsWith(".webp")) return ".webp";
            if (lower.endsWith(".gif")) return ".gif";
        }
        if (StringUtils.hasText(contentType)) {
            String mime = contentType.toLowerCase();
            if (mime.contains("png")) return ".png";
            if (mime.contains("jpeg") || mime.contains("jpg")) return ".jpg";
            if (mime.contains("webp")) return ".webp";
            if (mime.contains("gif")) return ".gif";
        }
        return ".png";
    }
}
