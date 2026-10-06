package com.taxoryn.module.gst.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * GST Authentication Purpose identifying the domain scope of an authenticated gateway session.
 * Ensures isolation between general GST returns, E-Way Bill operations, and E-Invoice operations.
 */
@Schema(description = "GST Authentication Purpose Scope")
public enum GstAuthenticationPurpose {

    @Schema(description = "General GST return filing, verification, and portal operations")
    GST,

    @Schema(description = "E-Way Bill generation, tracking, and vehicle updating operations")
    EWAY_BILL,

    @Schema(description = "E-Invoice Invoice Registration Number (IRN) generation and cancellation")
    E_INVOICE;

    public String getDisplayName() {
        return switch (this) {
            case GST -> "Goods & Services Tax (GST)";
            case EWAY_BILL -> "E-Way Bill Gateway";
            case E_INVOICE -> "E-Invoice (IRN) Gateway";
        };
    }
}
