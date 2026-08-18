package com.taxoryn.module.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Organization Settings Payload")
public class OrganizationSettingsDto {

    private UUID id;
    private UUID organizationId;
    private String timezone;
    private String dateFormat;
    private String currency;
    private Integer financialYearStartMonth;
    private boolean enableEmailNotifications;
    private boolean enableSmsNotifications;
    private boolean enableWhatsappNotifications;
    private String invoicePrefix;
    private boolean autoRemindersEnabled;
    private Instant createdAt;
    private Instant updatedAt;
}
