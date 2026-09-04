package com.taxoryn.core.security.upload;

import com.taxoryn.core.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Security inspector for ZIP archives and OpenXML document containers (DOCX, XLSX).
 * Protects against:
 * - Zip Bombs / Decompression Bombs
 * - Zip Slip / Path Traversal inside archive entry names
 * - Dangerous nested executables within archives
 * - Archive recursion exhaustion
 */
@Slf4j
@Component
public class ZipArchiveInspector {

    private static final int MAX_ENTRIES = 1000;
    private static final long MAX_TOTAL_UNCOMPRESSED_BYTES = 100 * 1024 * 1024; // 100 MB
    private static final double MAX_COMPRESSION_RATIO = 100.0; // 100:1 ratio limit

    /**
     * Inspects a ZIP/OpenXML byte payload to ensure it is structurally safe and not a decompression bomb.
     *
     * @param data binary archive bytes
     * @param originalFilename filename for error reporting
     * @throws BadRequestException if the archive is unsafe or violates security constraints
     */
    public void inspectZipArchive(byte[] data, String originalFilename) {
        if (data == null || data.length == 0) {
            return;
        }

        long compressedSize = data.length;
        long totalUncompressedBytes = 0;
        int entryCount = 0;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry;
            byte[] buffer = new byte[4096];

            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ENTRIES) {
                    log.warn("SECURITY ALERT: Archive '{}' exceeded maximum allowed entries ({})", originalFilename, MAX_ENTRIES);
                    throw new BadRequestException("Archive contains too many entries (maximum allowed: " + MAX_ENTRIES + ")");
                }

                String name = entry.getName();
                validateEntryName(name, originalFilename);

                // Read and count decompressed bytes safely
                int bytesRead;
                while ((bytesRead = zis.read(buffer)) != -1) {
                    totalUncompressedBytes += bytesRead;

                    if (totalUncompressedBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
                        log.warn("SECURITY ALERT: Archive '{}' exceeded max uncompressed size limit ({} bytes)",
                                originalFilename, MAX_TOTAL_UNCOMPRESSED_BYTES);
                        throw new BadRequestException("Decompression size exceeded maximum limit (100 MB)");
                    }

                    // Check compression ratio
                    if (compressedSize > 0) {
                        double ratio = (double) totalUncompressedBytes / compressedSize;
                        if (ratio > MAX_COMPRESSION_RATIO && totalUncompressedBytes > 1024 * 1024) {
                            log.warn("SECURITY ALERT: Potential Zip Bomb detected in '{}': compression ratio {} exceeds threshold {}",
                                    originalFilename, ratio, MAX_COMPRESSION_RATIO);
                            throw new BadRequestException("Malicious archive detected: decompression ratio exceeds safe limit (Zip Bomb defense)");
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (BadRequestException bre) {
            throw bre;
        } catch (IOException e) {
            log.warn("Failed to parse archive entries in '{}': {}", originalFilename, e.getMessage());
            throw new BadRequestException("Invalid or corrupted archive format");
        }
    }

    private void validateEntryName(String entryName, String originalFilename) {
        if (entryName == null || entryName.trim().isEmpty()) {
            throw new BadRequestException("Archive contains an entry with an empty filename");
        }

        // Null-byte injection check
        if (entryName.contains("\0") || entryName.contains("%00")) {
            log.warn("SECURITY ALERT: Null byte found in archive entry '{}' of file '{}'", entryName, originalFilename);
            throw new BadRequestException("Archive entry contains invalid null byte characters");
        }

        // Zip Slip / Path Traversal check
        if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")
                || entryName.contains(":\\") || entryName.contains(":/")) {
            log.warn("SECURITY ALERT: Path traversal / Zip Slip attempt detected in archive entry '{}' of file '{}'",
                    entryName, originalFilename);
            throw new BadRequestException("Archive entry contains illegal path traversal sequences: " + entryName);
        }

        // Disallow dangerous executable extensions within archives
        String lower = entryName.toLowerCase();
        if (lower.endsWith(".exe") || lower.endsWith(".bat") || lower.endsWith(".cmd")
                || lower.endsWith(".sh") || lower.endsWith(".ps1") || lower.endsWith(".vbs")
                || lower.endsWith(".jar") || lower.endsWith(".war") || lower.endsWith(".dll")
                || lower.endsWith(".so") || lower.endsWith(".jsp") || lower.endsWith(".php")
                || lower.endsWith(".scr") || lower.endsWith(".msi") || lower.endsWith(".com")) {
            log.warn("SECURITY ALERT: Dangerous executable entry '{}' found in archive '{}'", entryName, originalFilename);
            throw new BadRequestException("Archive contains disallowed executable file type: " + entryName);
        }
    }
}
