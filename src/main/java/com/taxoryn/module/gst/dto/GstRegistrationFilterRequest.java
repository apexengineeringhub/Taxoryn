package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gst.model.GstFilingFrequency;
import com.taxoryn.module.gst.model.GstRegistrationStatus;
import com.taxoryn.module.gst.model.GstRegistrationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter and pagination criteria for querying GST registrations")
public class GstRegistrationFilterRequest {

    @Schema(description = "Filter by client ID")
    private UUID clientId;

    @Schema(description = "Filter by location ID")
    private UUID locationId;

    @Schema(description = "Filter by registration status")
    private GstRegistrationStatus registrationStatus;

    @Schema(description = "Filter by taxpayer category")
    private GstRegistrationType registrationType;

    @Schema(description = "Filter by return filing frequency")
    private GstFilingFrequency filingFrequency;

    @Schema(description = "Filter by 2-digit state code")
    private String stateCode;

    @Schema(description = "Search keyword in GSTIN, legal name, or trade name")
    private String search;

    @Builder.Default
    @Schema(description = "Page index (0-based)", defaultValue = "0")
    private int page = 0;

    @Builder.Default
    @Schema(description = "Page size", defaultValue = "20")
    private int size = 20;

    @Builder.Default
    @Schema(description = "Sort property", defaultValue = "createdAt")
    private String sortBy = "createdAt";

    @Builder.Default
    @Schema(description = "Sort direction (ASC, DESC)", defaultValue = "DESC")
    private String sortDirection = "DESC";

    public Pageable toPageable() {
        Sort sort = "ASC".equalsIgnoreCase(sortDirection)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(Math.max(0, page), Math.max(1, size), sort);
    }
}
