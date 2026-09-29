package com.taxoryn.module.subscription.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for enterprise subscription inquiries and custom plan consultations")
public class EnterpriseInquiryRequest {

    @NotBlank(message = "Contact name is required")
    @Schema(description = "Full name of the contact person or lead partner", example = "CA Rajesh Sharma")
    private String contactName;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Please provide a valid business email address")
    @Schema(description = "Official email address", example = "rajesh@sharmaca.com")
    private String contactEmail;

    @Schema(description = "Contact phone number", example = "+91 9876543210")
    private String contactPhone;

    @Schema(description = "Practice or firm name", example = "Sharma & Associates LLP")
    private String firmName;

    @Schema(description = "Estimated team / staff count", example = "75")
    private Integer estimatedTeamSize;

    @Schema(description = "Estimated active client base count", example = "1200")
    private Integer estimatedClientCount;

    @Schema(description = "Specific enterprise requirements or custom integrations", example = "Custom ERP connector, dedicated database instance, SOC2 compliance")
    private String requirements;
}
