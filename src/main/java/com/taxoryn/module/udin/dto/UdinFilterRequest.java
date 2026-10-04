package com.taxoryn.module.udin.dto;

import com.taxoryn.module.udin.model.UdinDocumentType;
import com.taxoryn.module.udin.model.UdinStatus;
import com.taxoryn.module.udin.model.UdinVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "UDIN Search and Filter Criteria")
public class UdinFilterRequest {

    private String search;
    private UUID clientId;
    private UdinStatus status;
    private UdinVerificationStatus verificationStatus;
    private UdinDocumentType documentType;
    private String signatoryMembershipNo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
