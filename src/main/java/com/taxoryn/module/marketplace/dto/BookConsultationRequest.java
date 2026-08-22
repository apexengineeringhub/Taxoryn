package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceConsultationEntity.ConsultationMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer request to book a consultation slot from the Marketplace")
public class BookConsultationRequest {

    @NotNull(message = "Marketplace profile ID is required")
    private UUID marketplaceProfileId;

    @NotBlank(message = "Client name is required")
    private String clientName;

    @NotBlank(message = "Client email is required")
    @Email(message = "Invalid email address")
    private String clientEmail;

    @NotBlank(message = "Client phone is required")
    private String clientPhone;

    @NotBlank(message = "Topic is required")
    private String topic;

    @Builder.Default
    private ConsultationMode consultationMode = ConsultationMode.VIDEO;

    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    @NotBlank(message = "Start time is required")
    @Schema(example = "14:00")
    private String startTime;

    @NotBlank(message = "End time is required")
    @Schema(example = "14:30")
    private String endTime;

    private String notes;
}
