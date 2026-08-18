package com.taxoryn.module.organization.dto;

import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationEntity.SubscriptionPlan;
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
@Schema(description = "Organization Tenant Details Payload")
public class OrganizationDto {

    @Schema(description = "Unique organization identifier (UUID)")
    private UUID id;

    @Schema(description = "Organization display / brand name", example = "Apex & Associates Tax Advisors")
    private String name;

    @Schema(description = "Registered legal name", example = "Apex & Associates LLP")
    private String legalName;

    @Schema(description = "Trade / DBA name", example = "Apex Advisors")
    private String tradeName;

    @Schema(description = "Official contact email", example = "contact@apexadvisors.com")
    private String email;

    @Schema(description = "Official contact phone", example = "+919876543210")
    private String phone;

    @Schema(description = "Physical street address", example = "Suite 401, Nariman Point")
    private String address;

    @Schema(description = "City", example = "Mumbai")
    private String city;

    @Schema(description = "State", example = "Maharashtra")
    private String state;

    @Schema(description = "Country", example = "India")
    private String country;

    @Schema(description = "Postal Pincode", example = "400021")
    private String pincode;

    @Schema(description = "Permanent Account Number (PAN)", example = "ABCDE1234F")
    private String pan;

    @Schema(description = "Goods & Services Tax Identification Number (GSTIN)", example = "27ABCDE1234F1Z5")
    private String gstin;

    @Schema(description = "Tax or corporate registration number", example = "LLPIN-AAO-1234")
    private String taxRegistrationNumber;

    @Schema(description = "Tenant operational status")
    private OrganizationStatus status;

    @Schema(description = "Subscription tier plan")
    private SubscriptionPlan subscriptionPlan;

    @Schema(description = "Tenant configuration settings")
    private OrganizationSettingsDto settings;

    @Schema(description = "Account creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
