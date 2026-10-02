package com.taxoryn.module.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SubmitReviewRequest {
    @NotBlank
    private String resourceType;
    @NotNull
    private UUID resourceId;
}
