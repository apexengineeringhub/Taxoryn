package com.taxoryn.module.feedback.dto;

import com.taxoryn.module.feedback.entity.ApplicationFeedbackCategory;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer feedback about the Taxoryn application, not a tax practice review")
public class CreateApplicationFeedbackRequest {

    @NotNull(message = "Feedback type is required")
    private ApplicationFeedbackType type;

    @NotNull(message = "Feedback category is required")
    private ApplicationFeedbackCategory category;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @NotBlank(message = "Title is required")
    @Size(max = 160, message = "Title cannot exceed 160 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 4000, message = "Description cannot exceed 4000 characters")
    private String description;
}
