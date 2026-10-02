package com.taxoryn.module.lead.dto;

import com.taxoryn.module.lead.entity.PracticeLeadEntity.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class PracticeLeadRequest {
    private LeadType leadType = LeadType.INDIVIDUAL;
    @NotBlank @Size(max = 255) private String name;
    @Size(max = 255) private String businessName;
    @Email @Size(max = 255) private String email;
    @Size(max = 50) private String phone;
    private LeadSource source = LeadSource.OTHER;
    private LeadStatus status = LeadStatus.NEW;
    private LeadPriority priority = LeadPriority.MEDIUM;
    @Size(max = 100) private String interestedServiceCode;
    private String description;
    private UUID assignedEmployeeId;
    private Instant nextFollowUpAt;
    @Size(max = 500) private String lostReason;
}
