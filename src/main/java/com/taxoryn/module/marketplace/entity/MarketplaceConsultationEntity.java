package com.taxoryn.module.marketplace.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "marketplace_consultations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceConsultationEntity extends AuditableEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "marketplace_profile_id", nullable = false)
    private UUID marketplaceProfileId;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "client_email", nullable = false)
    private String clientEmail;

    @Column(name = "client_phone", nullable = false, length = 20)
    private String clientPhone;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_mode", nullable = false, length = 50)
    @Builder.Default
    private ConsultationMode consultationMode = ConsultationMode.VIDEO;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "start_time", nullable = false, length = 10)
    private String startTime;

    @Column(name = "end_time", nullable = false, length = 10)
    private String endTime;

    @Column(name = "fee_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 50)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_status", nullable = false, length = 50)
    @Builder.Default
    private ConsultationStatus consultationStatus = ConsultationStatus.SCHEDULED;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum ConsultationMode {
        VIDEO,
        PHONE,
        IN_PERSON
    }

    public enum PaymentStatus {
        PENDING,
        PAID,
        WAIVED,
        REFUNDED
    }

    public enum ConsultationStatus {
        SCHEDULED,
        COMPLETED,
        CANCELLED,
        NO_SHOW
    }
}
