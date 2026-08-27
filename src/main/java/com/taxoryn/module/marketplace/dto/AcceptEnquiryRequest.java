package com.taxoryn.module.marketplace.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptEnquiryRequest {
    private String notes;
    private Integer estimatedDaysToComplete;
}
