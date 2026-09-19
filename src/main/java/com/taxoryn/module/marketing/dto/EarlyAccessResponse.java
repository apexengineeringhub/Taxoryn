package com.taxoryn.module.marketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarlyAccessResponse {

    private UUID id;
    private String message;
    private String status;
    private String email;
    private Instant createdAt;

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + domain;
        }
        return local.substring(0, 2) + "***@" + domain;
    }
}
