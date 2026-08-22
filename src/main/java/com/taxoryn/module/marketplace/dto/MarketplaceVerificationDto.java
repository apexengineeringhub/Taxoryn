package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practitioner KYC / Credential Verification Record DTO")
public class MarketplaceVerificationDto {

    private UUID id;
    private UUID organizationId;
    private String organizationName;
    private UUID marketplaceProfileId;
    private String professionalBody;
    private String membershipNumber;
    private String copNumber;
    private String firmRegistrationNumber;
    private String documentUrl;
    private VerificationStatus verificationStatus;
    private String rejectionReason;
    private Instant verifiedAt;
    private String verifiedBy;
    private Instant createdAt;
}
