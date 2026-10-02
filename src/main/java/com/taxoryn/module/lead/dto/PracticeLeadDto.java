package com.taxoryn.module.lead.dto;

import com.taxoryn.module.lead.entity.PracticeLeadEntity.*;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class PracticeLeadDto {
    private UUID id;
    private LeadType leadType;
    private String name;
    private String businessName;
    private String email;
    private String phone;
    private LeadSource source;
    private LeadStatus status;
    private LeadPriority priority;
    private String interestedServiceCode;
    private String interestedServiceName;
    private String description;
    private UUID assignedEmployeeId;
    private String assignedEmployeeName;
    private Instant nextFollowUpAt;
    private UUID convertedClientId;
    private Instant convertedAt;
    private Instant createdAt;
    private String lostReason;
}
