package com.taxoryn.module.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveFeedbackRequest {
    @NotBlank(message = "Resolution note is required")
    @Size(max = 4000, message = "Resolution note cannot exceed 4000 characters")
    private String resolutionNote;
}
