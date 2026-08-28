package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.EnquiryStatus;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnquiryMessageThreadDto {
    private UUID enquiryId;
    private String referenceNumber;
    private EnquiryStatus enquiryStatus;
    private String clientName;
    private String practiceName;
    private String assignedEmployeeName;
    private long unreadCountForCustomer;
    private long unreadCountForPractice;
    private boolean isMessagingActive;
    private List<EnquiryMessageDto> messages;
}
