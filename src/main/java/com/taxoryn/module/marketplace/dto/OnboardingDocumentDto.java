package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceOnboardingDocumentEntity.DocumentType;
import com.taxoryn.module.marketplace.entity.MarketplaceOnboardingDocumentEntity.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KYC Onboarding Document DTO")
public class OnboardingDocumentDto {

    private UUID id;
    private UUID onboardingId;
    private DocumentType documentType;
    private String documentName;
    private String filePath;
    private Long fileSizeBytes;
    private String contentType;
    private Boolean isRequired;
    private VerificationStatus verificationStatus;
    private String rejectionReason;
    private Instant verifiedAt;
    private String verifiedBy;
    private Instant createdAt;
}
