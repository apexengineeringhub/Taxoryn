package com.taxoryn.core.security.upload;

import com.taxoryn.core.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * High-security multi-layer file upload validator for Taxoryn.
 * Performs strict validation across:
 * - Filename sanitization, null byte rejection, path traversal rejection
 * - Extension allowlisting & double-extension attack defense
 * - MIME-type validation
 * - Magic byte / File signature verification
 * - Disguised binary / Polyglot detection
 * - Archive structural integrity & Zip Bomb inspection
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileValidator {

    private static final long DEFAULT_MAX_FILE_SIZE = 25 * 1024 * 1024; // 25 MB

    // Allowed extensions
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "webp",
            "doc", "docx", "xls", "xlsx", "csv", "txt", "zip"
    );

    // Explicit dangerous extensions
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "ps1", "vbs", "js", "jar", "war", "ear",
            "dll", "so", "dylib", "bin", "msi", "scr", "pif", "com", "cpl", "hta",
            "jsp", "jspx", "php", "php3", "php4", "php5", "phtml", "asp", "aspx",
            "cgi", "pl", "py", "rb", "vbe", "wsf", "wsh"
    );

    // Magic Byte Signatures
    private static final byte[] PDF_MAGIC = new byte[]{(byte) 0x25, (byte) 0x50, (byte) 0x44, (byte) 0x46, (byte) 0x2D}; // %PDF-
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] ZIP_MAGIC = new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04}; // PK\x03\x04
    private static final byte[] ZIP_EMPTY_MAGIC = new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x05, (byte) 0x06}; // PK\x05\x06
    private static final byte[] ZIP_SPANNED_MAGIC = new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x07, (byte) 0x08}; // PK\x07\x08
    private static final byte[] OLE_MAGIC = new byte[]{(byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1}; // DOC/XLS legacy OLE

    // Disguised Executable Signatures
    private static final byte[] MZ_HEADER = new byte[]{(byte) 0x4D, (byte) 0x5A}; // Windows PE
    private static final byte[] ELF_HEADER = new byte[]{(byte) 0x7F, (byte) 0x45, (byte) 0x4C, (byte) 0x46}; // Linux ELF
    private static final byte[] JAVA_CLASS_HEADER = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}; // Java Class

    private final ZipArchiveInspector zipArchiveInspector;

    /**
     * Validates the uploaded file against security constraints.
     *
     * @param originalFilename original name of the uploaded file
     * @param declaredContentType Content-Type header supplied with the request
     * @param data binary payload of the file
     * @throws BadRequestException if any validation check fails
     */
    public void validate(String originalFilename, String declaredContentType, byte[] data) {
        // 1. Basic non-empty and size check
        if (data == null || data.length == 0) {
            throw new BadRequestException("Uploaded file must not be empty");
        }
        if (data.length > DEFAULT_MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed limit of 25 MB");
        }

        // 2. Filename sanitization & character validation
        String sanitizedFilename = validateAndSanitizeFilename(originalFilename);

        // 3. Extension extraction & validation (including double-extension defense)
        String extension = validateExtension(sanitizedFilename);

        // 4. Disguised binary / Polyglot check
        checkDisguisedBinary(data, sanitizedFilename);

        // 5. Magic Byte / File signature matching against declared extension
        validateMagicBytes(data, extension, sanitizedFilename);

        // 6. Deep archive inspection for ZIP and OpenXML document containers
        if ("zip".equals(extension) || "docx".equals(extension) || "xlsx".equals(extension)) {
            zipArchiveInspector.inspectZipArchive(data, sanitizedFilename);
        }
    }

    private String validateAndSanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new BadRequestException("Filename must not be empty");
        }

        // Null-byte injection check
        if (filename.contains("\0") || filename.contains("%00")) {
            log.warn("SECURITY ALERT: Null byte detected in filename: {}", filename);
            throw new BadRequestException("Filename contains invalid null byte characters");
        }

        // Path traversal check
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\") || filename.contains(":")) {
            log.warn("SECURITY ALERT: Path traversal detected in filename: {}", filename);
            throw new BadRequestException("Filename contains illegal path traversal characters");
        }

        // Control characters check (< 0x20)
        for (char c : filename.toCharArray()) {
            if (c < 32 || c == 127) {
                log.warn("SECURITY ALERT: Control character detected in filename: {}", filename);
                throw new BadRequestException("Filename contains illegal control characters");
            }
        }

        if (filename.length() > 255) {
            throw new BadRequestException("Filename exceeds maximum length of 255 characters");
        }

        return filename.trim();
    }

    private String validateExtension(String filename) {
        String lowerFilename = filename.toLowerCase(Locale.ROOT);
        String[] parts = lowerFilename.split("\\.");

        if (parts.length < 2) {
            throw new BadRequestException("Filename must have a valid file extension");
        }

        // Double extension & dangerous extension checks
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (DANGEROUS_EXTENSIONS.contains(part)) {
                log.warn("SECURITY ALERT: Dangerous extension token '{}' detected in filename: {}", part, filename);
                throw new BadRequestException("Disallowed or dangerous file extension detected in filename");
            }
        }

        String mainExt = parts[parts.length - 1].trim();
        if (!ALLOWED_EXTENSIONS.contains(mainExt)) {
            log.warn("SECURITY ALERT: Unsupported file extension '{}' in filename: {}", mainExt, filename);
            throw new BadRequestException("Unsupported file extension: ." + mainExt + ". Allowed extensions: PDF, PNG, JPG, JPEG, WEBP, DOC, DOCX, XLS, XLSX, CSV, TXT, ZIP");
        }

        return mainExt;
    }

    private void checkDisguisedBinary(byte[] data, String filename) {
        if (data.length >= 2 && data[0] == MZ_HEADER[0] && data[1] == MZ_HEADER[1]) {
            log.warn("SECURITY ALERT: Executable Windows PE binary (MZ header) disguised as document: {}", filename);
            throw new BadRequestException("Executable binary disguised as document is not allowed");
        }

        if (data.length >= 4 && data[0] == ELF_HEADER[0] && data[1] == ELF_HEADER[1]
                && data[2] == ELF_HEADER[2] && data[3] == ELF_HEADER[3]) {
            log.warn("SECURITY ALERT: Executable Linux ELF binary disguised as document: {}", filename);
            throw new BadRequestException("Executable binary disguised as document is not allowed");
        }

        if (data.length >= 4 && data[0] == JAVA_CLASS_HEADER[0] && data[1] == JAVA_CLASS_HEADER[1]
                && data[2] == JAVA_CLASS_HEADER[2] && data[3] == JAVA_CLASS_HEADER[3]) {
            log.warn("SECURITY ALERT: Compiled Java bytecode disguised as document: {}", filename);
            throw new BadRequestException("Executable compiled bytecode disguised as document is not allowed");
        }
    }

    private void validateMagicBytes(byte[] data, String extension, String filename) {
        switch (extension) {
            case "pdf" -> {
                if (!startsWith(data, PDF_MAGIC)) {
                    log.warn("SECURITY ALERT: Magic byte mismatch for PDF file: {}", filename);
                    throw new BadRequestException("File content does not match valid PDF document signature (%PDF-)");
                }
            }
            case "png" -> {
                if (!startsWith(data, PNG_MAGIC)) {
                    log.warn("SECURITY ALERT: Magic byte mismatch for PNG image: {}", filename);
                    throw new BadRequestException("File content does not match valid PNG image signature");
                }
            }
            case "jpg", "jpeg" -> {
                if (!startsWith(data, JPEG_MAGIC)) {
                    log.warn("SECURITY ALERT: Magic byte mismatch for JPEG image: {}", filename);
                    throw new BadRequestException("File content does not match valid JPEG image signature");
                }
            }
            case "webp" -> {
                if (data.length < 12 || !startsWith(data, "RIFF".getBytes(StandardCharsets.US_ASCII))
                        || !matchesAtOffset(data, 8, "WEBP".getBytes(StandardCharsets.US_ASCII))) {
                    log.warn("SECURITY ALERT: Magic byte mismatch for WEBP image: {}", filename);
                    throw new BadRequestException("File content does not match valid WEBP image signature");
                }
            }
            case "docx", "xlsx", "zip" -> {
                if (!startsWith(data, ZIP_MAGIC) && !startsWith(data, ZIP_EMPTY_MAGIC) && !startsWith(data, ZIP_SPANNED_MAGIC)) {
                    log.warn("SECURITY ALERT: Magic byte mismatch for archive / OpenXML document: {}", filename);
                    throw new BadRequestException("File content does not match valid PK archive / OpenXML document signature");
                }
            }
            case "doc", "xls" -> {
                // Allow legacy OLE or OpenXML format renamed
                if (!startsWith(data, OLE_MAGIC) && !startsWith(data, ZIP_MAGIC)) {
                    log.warn("SECURITY ALERT: Magic byte mismatch for Office document: {}", filename);
                    throw new BadRequestException("File content does not match valid Microsoft Office document signature");
                }
            }
            case "csv", "txt" -> {
                validatePlainText(data, filename);
            }
            default -> throw new BadRequestException("Unsupported file type: " + extension);
        }
    }

    private void validatePlainText(byte[] data, String filename) {
        // Plain text must not contain binary nulls or non-printable ASCII control characters
        // Allowed control characters: \t (9), \n (10), \r (13)
        int inspectLength = Math.min(data.length, 8192);
        for (int i = 0; i < inspectLength; i++) {
            byte b = data[i];
            if (b == 0) {
                log.warn("SECURITY ALERT: Binary null byte detected in plain text/CSV file: {}", filename);
                throw new BadRequestException("Plain text / CSV file contains invalid binary content");
            }
            if (b > 0 && b < 32 && b != 9 && b != 10 && b != 13) {
                log.warn("SECURITY ALERT: Suspicious control byte 0x{} in text file: {}", Integer.toHexString(b), filename);
                throw new BadRequestException("Plain text / CSV file contains invalid control characters");
            }
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private boolean matchesAtOffset(byte[] data, int offset, byte[] target) {
        if (data.length < offset + target.length) return false;
        for (int i = 0; i < target.length; i++) {
            if (data[offset + i] != target[i]) return false;
        }
        return true;
    }
}
