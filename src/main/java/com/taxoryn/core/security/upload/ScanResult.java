package com.taxoryn.core.security.upload;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScanResult {

    public enum Status {
        CLEAN,
        INFECTED,
        FAILED
    }

    private final Status status;
    private final String threatName;
    private final String scannerName;
    private final String details;

    public boolean isClean() {
        return status == Status.CLEAN;
    }

    public boolean isInfected() {
        return status == Status.INFECTED;
    }

    public boolean isFailed() {
        return status == Status.FAILED;
    }

    public static ScanResult clean(String scannerName) {
        return ScanResult.builder()
                .status(Status.CLEAN)
                .scannerName(scannerName)
                .details("No threat detected")
                .build();
    }

    public static ScanResult infected(String threatName, String scannerName, String details) {
        return ScanResult.builder()
                .status(Status.INFECTED)
                .threatName(threatName)
                .scannerName(scannerName)
                .details(details != null ? details : "Threat detected: " + threatName)
                .build();
    }

    public static ScanResult failed(String scannerName, String reason) {
        return ScanResult.builder()
                .status(Status.FAILED)
                .scannerName(scannerName)
                .details(reason != null ? reason : "Malware scan failed")
                .build();
    }
}
