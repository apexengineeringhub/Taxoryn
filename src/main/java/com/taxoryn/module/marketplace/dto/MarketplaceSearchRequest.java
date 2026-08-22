package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.ProfessionalType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Search and filter criteria for public marketplace")
public class MarketplaceSearchRequest {

    @Schema(description = "City or metropolitan area filter", example = "Mumbai")
    private String city;

    @Schema(description = "Professional category filter", example = "CHARTERED_ACCOUNTANT")
    private ProfessionalType professionalType;

    @Schema(description = "Specialization keyword", example = "GST_FILING")
    private String specialization;

    @Schema(description = "Filter only KYC verified practitioners")
    private Boolean verifiedOnly;

    @Schema(description = "Keyword search for firm name, headline, bio")
    private String search;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 12;

    @Builder.Default
    private String sortBy = "averageRating";

    @Builder.Default
    private String sortDirection = "desc";

    public Pageable toPageable() {
        Sort sort = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        return PageRequest.of(Math.max(0, page), Math.max(1, size), sort);
    }
}
