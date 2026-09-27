package com.taxoryn.module.billing.dto;

import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Invoice Status Payload")
public class UpdateInvoiceStatusRequest {

    @NotNull(message = "Invoice status is required")
    private InvoiceStatus status;

    private String notes;
}
