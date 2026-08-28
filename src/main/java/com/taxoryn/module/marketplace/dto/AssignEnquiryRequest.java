package com.taxoryn.module.marketplace.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignEnquiryRequest {
    @NotNull(message = "Assigned employee ID is required")
    private UUID assignedEmployeeId;

    private String assignmentNotes;
}
