package com.taxoryn.core.security.upload;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Built-in pattern scanner that detects the standard EICAR test string and embedded executable binary signatures.
 */
@Slf4j
@Component
@Order(10)
public class EicarPatternScanner implements MalwareScanner {

    private static final String SCANNER_NAME = "Taxoryn-Signature-Scanner";

    // Standard EICAR test virus string (68 chars)
    private static final String EICAR_TEST_STRING =
            "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";

    private static final byte[] MZ_HEADER = new byte[]{(byte) 0x4D, (byte) 0x5A}; // 'MZ' Windows PE Executable
    private static final byte[] ELF_HEADER = new byte[]{(byte) 0x7F, (byte) 0x45, (byte) 0x4C, (byte) 0x46}; // '\x7FELF'
    private static final byte[] JAVA_CLASS_HEADER = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}; // Java class

    @Override
    public ScanResult scan(byte[] data, String filename) {
        if (data == null || data.length == 0) {
            return ScanResult.clean(SCANNER_NAME);
        }

        try {
            // 1. Check for EICAR standard signature in ASCII / UTF-8
            String content = new String(data, StandardCharsets.ISO_8859_1);
            if (content.contains(EICAR_TEST_STRING)) {
                log.warn("SECURITY ALERT: EICAR test signature detected in uploaded file '{}'", filename);
                return ScanResult.infected("EICAR-Standard-AV-Test-Signature", SCANNER_NAME,
                        "Standard EICAR antivirus test file signature detected.");
            }

            // 2. Check for disguised Windows PE executable binary (MZ header)
            if (data.length >= 2 && data[0] == MZ_HEADER[0] && data[1] == MZ_HEADER[1]) {
                log.warn("SECURITY ALERT: Disguised Windows PE Executable (MZ header) detected in file '{}'", filename);
                return ScanResult.infected("Win32/PE.Executable.Disguised", SCANNER_NAME,
                        "Disguised executable binary (Windows PE header) detected.");
            }

            // 3. Check for disguised Linux ELF binary
            if (data.length >= 4 && data[0] == ELF_HEADER[0] && data[1] == ELF_HEADER[1]
                    && data[2] == ELF_HEADER[2] && data[3] == ELF_HEADER[3]) {
                log.warn("SECURITY ALERT: Disguised Linux ELF binary detected in file '{}'", filename);
                return ScanResult.infected("Linux/ELF.Binary.Disguised", SCANNER_NAME,
                        "Disguised executable binary (Linux ELF header) detected.");
            }

            // 4. Check for disguised Java class / Mach-O binary
            if (data.length >= 4 && data[0] == JAVA_CLASS_HEADER[0] && data[1] == JAVA_CLASS_HEADER[1]
                    && data[2] == JAVA_CLASS_HEADER[2] && data[3] == JAVA_CLASS_HEADER[3]) {
                log.warn("SECURITY ALERT: Disguised Java class / Mach-O binary detected in file '{}'", filename);
                return ScanResult.infected("Binary/Compiled.Class.Disguised", SCANNER_NAME,
                        "Disguised compiled binary bytecode detected.");
            }

            return ScanResult.clean(SCANNER_NAME);
        } catch (Exception e) {
            log.error("Malware scan error during signature scanning of '{}': {}", filename, e.getMessage(), e);
            // FAIL-CLOSED: Any scanner error must result in scan failure to protect tenant data
            return ScanResult.failed(SCANNER_NAME, "Scanner error: " + e.getMessage());
        }
    }

    @Override
    public String getScannerName() {
        return SCANNER_NAME;
    }
}
