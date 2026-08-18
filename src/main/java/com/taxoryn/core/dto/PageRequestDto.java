package com.taxoryn.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pagination and Sorting Parameters")
public class PageRequestDto {

    @Min(value = 0, message = "Page index must be greater than or equal to 0")
    @Builder.Default
    @Schema(description = "Zero-based page index", defaultValue = "0", example = "0")
    private int page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    @Builder.Default
    @Schema(description = "Number of records per page (max 100)", defaultValue = "20", example = "20")
    private int size = 20;

    @Schema(description = "Field name to sort by", defaultValue = "createdAt", example = "createdAt")
    @Builder.Default
    private String sortBy = "createdAt";

    @Schema(description = "Sort direction (ASC or DESC)", defaultValue = "DESC", allowableValues = {"ASC", "DESC"}, example = "DESC")
    @Builder.Default
    private String sortDirection = "DESC";

    public Pageable toPageable() {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String field = StringUtils.hasText(sortBy) ? sortBy : "createdAt";
        return PageRequest.of(page, size, Sort.by(direction, field));
    }
}
