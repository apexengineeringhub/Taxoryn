package com.taxoryn.module.marketplace.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelEnquiryRequest {
    private String cancellationReason;
}
