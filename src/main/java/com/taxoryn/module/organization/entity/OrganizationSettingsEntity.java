package com.taxoryn.module.organization.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "organization_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSettingsEntity extends AuditableEntity {

    @Column(name = "organization_id", nullable = false, unique = true)
    private UUID organizationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", insertable = false, updatable = false)
    private OrganizationEntity organization;

    @Column(name = "timezone", nullable = false, length = 50)
    @Builder.Default
    private String timezone = "Asia/Kolkata";

    @Column(name = "date_format", nullable = false, length = 30)
    @Builder.Default
    private String dateFormat = "DD/MM/YYYY";

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "financial_year_start_month", nullable = false)
    @Builder.Default
    private Integer financialYearStartMonth = 4; // April for India

    @Column(name = "enable_email_notifications", nullable = false)
    @Builder.Default
    private boolean enableEmailNotifications = true;

    @Column(name = "enable_sms_notifications", nullable = false)
    @Builder.Default
    private boolean enableSmsNotifications = false;

    @Column(name = "enable_whatsapp_notifications", nullable = false)
    @Builder.Default
    private boolean enableWhatsappNotifications = false;

    @Column(name = "invoice_prefix", nullable = false, length = 20)
    @Builder.Default
    private String invoicePrefix = "INV";

    @Column(name = "auto_reminders_enabled", nullable = false)
    @Builder.Default
    private boolean autoRemindersEnabled = true;

    public static OrganizationSettingsEntity createDefault(UUID organizationId) {
        return OrganizationSettingsEntity.builder()
                .organizationId(organizationId)
                .timezone("Asia/Kolkata")
                .dateFormat("DD/MM/YYYY")
                .currency("INR")
                .financialYearStartMonth(4)
                .enableEmailNotifications(true)
                .enableSmsNotifications(false)
                .enableWhatsappNotifications(false)
                .invoicePrefix("INV")
                .autoRemindersEnabled(true)
                .build();
    }
}
