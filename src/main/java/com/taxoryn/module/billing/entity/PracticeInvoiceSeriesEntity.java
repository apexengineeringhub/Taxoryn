package com.taxoryn.module.billing.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "practice_invoice_series", uniqueConstraints = {
    @UniqueConstraint(name = "uq_practice_invoice_series", columnNames = {"organization_id", "financial_year", "series_prefix"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeInvoiceSeriesEntity extends TenantAuditableEntity {

    @Column(name = "financial_year", nullable = false, length = 20)
    private String financialYear;

    @Column(name = "series_prefix", nullable = false, length = 50)
    @Builder.Default
    private String seriesPrefix = "INV";

    @Column(name = "delimiter", nullable = false, length = 5)
    @Builder.Default
    private String delimiter = "-";

    @Column(name = "current_sequence", nullable = false)
    @Builder.Default
    private Long currentSequence = 0L;
}
