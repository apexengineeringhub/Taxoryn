package com.taxoryn.module.document.storage;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.InternalServerException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "taxoryn.storage", name = "provider", havingValue = "LOCAL", matchIfMissing = true)
public class LocalDocumentStorageService implements DocumentStorageService {

    private final StorageProperties storageProperties;
    private Path rootLocation;

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(storageProperties.getLocal().getBaseDir()).toAbsolutePath().normalize();
            Files.createDirectories(this.rootLocation);
            log.info("Initialized LocalDocumentStorageService at root path: {}", this.rootLocation);
        } catch (IOException e) {
            throw new InternalServerException("Could not initialize local document storage directory: " + e.getMessage());
        }
    }

    @Override
    public String store(UUID organizationId, String originalFilename, String contentType, byte[] data) {
        if (data == null || data.length == 0) {
            throw new BadRequestException("Cannot store empty document file");
        }

        String safeExt = getSafeExtension(originalFilename);
        LocalDate now = LocalDate.now();
        String orgPrefix = organizationId != null ? "org_" + organizationId : "platform";
        String relativeOrgDir = orgPrefix + "/" + now.getYear() + "/" + String.format("%02d", now.getMonthValue());

        Path targetDir = this.rootLocation.resolve(relativeOrgDir).normalize();

        try {
            Files.createDirectories(targetDir);
            // Opaque UUID filename - prevents any client name, PAN, or sensitive metadata leakage in storage keys
            String uniqueFilename = UUID.randomUUID() + safeExt;
            Path destinationFile = targetDir.resolve(uniqueFilename).normalize();

            // Guard against path traversal attacks
            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new BadRequestException("Invalid storage destination path (path traversal detected)");
            }

            Files.write(destinationFile, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            String storageKey = relativeOrgDir + "/" + uniqueFilename;
            log.debug("Stored document locally with key: {}", storageKey);
            return storageKey;
        } catch (IOException e) {
            log.error("Failed to write file to local disk: {}", e.getMessage(), e);
            throw new InternalServerException("Failed to store file: " + e.getMessage());
        }
    }

    @Override
    public byte[] retrieve(String storageKey) {
        Path filePath = resolveAndValidatePath(storageKey);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath) || !Files.isReadable(filePath)) {
            throw new ResourceNotFoundException("Document file", "storageKey", "[REDACTED]");
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read file from disk for key {}: {}", storageKey, e.getMessage(), e);
            throw new InternalServerException("Failed to retrieve document content: " + e.getMessage());
        }
    }

    @Override
    public void delete(String storageKey) {
        if (!StringUtils.hasText(storageKey)) return;
        try {
            Path filePath = resolveAndValidatePath(storageKey);
            Files.deleteIfExists(filePath);
            log.debug("Deleted file from local storage: {}", storageKey);
        } catch (BadRequestException e) {
            log.warn("Ignored invalid storage key delete attempt: {}", e.getMessage());
        } catch (IOException e) {
            log.warn("Failed to delete local document file: {}", e.getMessage());
        }
    }

    @Override
    public boolean exists(String storageKey) {
        if (!StringUtils.hasText(storageKey)) return false;
        try {
            Path filePath = resolveAndValidatePath(storageKey);
            return Files.exists(filePath) && Files.isRegularFile(filePath) && Files.isReadable(filePath);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getStorageProviderName() {
        return "LOCAL";
    }

    private Path resolveAndValidatePath(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new BadRequestException("Storage key must not be empty");
        }
        // Reject path traversal indicators in raw key string
        if (storageKey.contains("..") || storageKey.contains("\0") || storageKey.contains("\\")
                || storageKey.contains("%2e") || storageKey.contains("%2E")
                || storageKey.contains("%2f") || storageKey.contains("%2F")
                || storageKey.contains("%5c") || storageKey.contains("%5C")) {
            throw new BadRequestException("Invalid storage key (path traversal characters detected)");
        }
        Path resolved = this.rootLocation.resolve(storageKey).normalize();
        if (!resolved.startsWith(this.rootLocation) || resolved.equals(this.rootLocation)) {
            throw new BadRequestException("Invalid storage key (path traversal detected)");
        }
        return resolved;
    }

    private String getSafeExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return ".bin";
        }
        String clean = Paths.get(filename).getFileName().toString();
        int dotIndex = clean.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < clean.length() - 1) {
            String ext = clean.substring(dotIndex).toLowerCase();
            // Validate extension is purely alphanumeric with leading dot, max 10 chars
            if (ext.matches("^\\.[a-z0-9]{1,10}$")) {
                return ext;
            }
        }
        return ".bin";
    }
}
