package com.taxoryn.module.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Organization Settings Request Payload")
public class UpdateOrganizationSettingsRequest {

    @NotBlank(message = "Timezone is required")
    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    @Schema(description = "Timezone identifier", example = "Asia/Kolkata")
    private String timezone;

    @NotBlank(message = "Date format is required")
    @Size(max = 30, message = "Date format cannot exceed 30 characters")
    @Schema(description = "Date display format", example = "DD/MM/YYYY")
    private String dateFormat;

    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 10, message = "Currency code must be 3-10 characters")
    @Schema(description = "Currency ISO code", example = "INR")
    private String currency;

    @NotNull(message = "Financial year start month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    @Schema(description = "Financial year start month (1-12, 4 for April)", example = "4")
    private Integer financialYearStartMonth;

    @Schema(description = "Enable email notification dispatch")
    private Boolean enableEmailNotifications;

    @Schema(description = "Enable SMS notification dispatch")
    private Boolean enableSmsNotifications;

    @Schema(description = "Enable WhatsApp notification dispatch")
    private Boolean enableWhatsappNotifications;

    @Size(max = 20, message = "Invoice prefix cannot exceed 20 characters")
    @Schema(description = "Prefix for invoice numbering", example = "TAX/2026/")
    private String invoicePrefix;

    @Schema(description = "Enable automatic statutory compliance reminders")
    private Boolean autoRemindersEnabled;
}
