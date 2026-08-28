package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.EnquiryRejectionReason;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectEnquiryRequest {
    @NotNull(message = "Rejection reason is required")
    private EnquiryRejectionReason rejectionReason;

    private String rejectionNote;
}
