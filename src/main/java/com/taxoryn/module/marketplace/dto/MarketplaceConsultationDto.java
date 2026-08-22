package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceConsultationEntity.ConsultationMode;
import com.taxoryn.module.marketplace.entity.MarketplaceConsultationEntity.ConsultationStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceConsultationEntity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Marketplace Booked Consultation DTO")
public class MarketplaceConsultationDto {

    private UUID id;
    private UUID organizationId;
    private UUID marketplaceProfileId;
    private String practiceDisplayName;
    private UUID leadId;
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private String topic;
    private ConsultationMode consultationMode;
    private String meetingLink;
    private LocalDate bookingDate;
    private String startTime;
    private String endTime;
    private BigDecimal feeAmount;
    private PaymentStatus paymentStatus;
    private ConsultationStatus consultationStatus;
    private UUID assignedEmployeeId;
    private String assignedEmployeeName;
    private String notes;
}
