package com.taxoryn.module.organization.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignLocationEmployeesRequest {

    @NotNull(message = "Employee IDs list is required")
    private List<UUID> employeeIds;
}
