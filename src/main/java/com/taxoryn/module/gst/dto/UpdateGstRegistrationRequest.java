package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gst.model.GstFilingFrequency;
import com.taxoryn.module.gst.model.GstRegistrationStatus;
import com.taxoryn.module.gst.model.GstRegistrationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request model to update an existing GST Registration profile")
public class UpdateGstRegistrationRequest {

    @Schema(description = "Servicing location ID")
    private UUID locationId;

    @Schema(description = "Legal business name")
    private String legalName;

    @Schema(description = "Trade or brand name")
    private String tradeName;

    @Schema(description = "Taxpayer registration category")
    private GstRegistrationType registrationType;

    @Schema(description = "Statutory registration status")
    private GstRegistrationStatus registrationStatus;

    @Schema(description = "Date of GST registration")
    private LocalDate registrationDate;

    @Schema(description = "2-digit state code")
    private String stateCode;

    @Schema(description = "Tax jurisdiction")
    private String jurisdiction;

    @Schema(description = "Statutory return filing frequency")
    private GstFilingFrequency filingFrequency;

    @Schema(description = "Effective start date")
    private LocalDate effectiveFrom;

    @Schema(description = "Effective end date")
    private LocalDate effectiveTo;

    @Schema(description = "Active flag")
    private Boolean active;
}
