package com.taxoryn.core.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Standard Paginated Response container.
 *
 * @param <T> Item type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated Response Payload")
public class PagedResponse<T> {

    @Schema(description = "Current page content elements")
    private List<T> content;

    @Schema(description = "Zero-based page index", example = "0")
    private int pageNumber;

    @Schema(description = "Size of each page", example = "20")
    private int pageSize;

    @Schema(description = "Total elements count across all pages", example = "105")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "6")
    private int totalPages;

    @Schema(description = "Is this the first page?", example = "true")
    private boolean isFirst;

    @Schema(description = "Is this the last page?", example = "false")
    private boolean isLast;

    @Schema(description = "Has next page available?", example = "true")
    private boolean hasNext;

    @Schema(description = "Has previous page available?", example = "false")
    private boolean hasPrevious;

    public static <E> PagedResponse<E> of(Page<E> page) {
        return PagedResponse.<E>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    public static <E, D> PagedResponse<D> of(Page<E> page, Function<E, D> mapper) {
        List<D> dtos = page.getContent().stream().map(mapper).collect(Collectors.toList());
        return PagedResponse.<D>builder()
                .content(dtos)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
