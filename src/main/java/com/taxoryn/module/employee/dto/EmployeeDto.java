package com.taxoryn.module.employee.dto;

import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Employee Profile Details")
public class EmployeeDto {

    @Schema(description = "Employee ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID id;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Optional linked user account ID")
    private UUID userId;

    @Schema(description = "Employee identifier code", example = "EMP-001")
    private String employeeCode;

    @Schema(description = "First name", example = "Rohan")
    private String firstName;

    @Schema(description = "Last name", example = "Deshmukh")
    private String lastName;

    @Schema(description = "Full name", example = "Rohan Deshmukh")
    private String fullName;

    @Schema(description = "Official email address", example = "rohan.d@taxpractice.com")
    private String email;

    @Schema(description = "Contact phone number", example = "+919876543210")
    private String phone;

    @Schema(description = "Department name", example = "Taxation")
    private String department;

    @Schema(description = "Designation/Job Title", example = "Senior Tax Associate")
    private String designation;

    @Schema(description = "Current employment status", example = "ACTIVE")
    private EmployeeStatus status;

    @Schema(description = "Date of joining", example = "2024-04-01")
    private LocalDate joiningDate;

    @Schema(description = "Reporting manager ID")
    private UUID managerId;

    @Schema(description = "Reporting manager full name", example = "Vikram Verma")
    private String managerName;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last updated timestamp")
    private Instant updatedAt;
}
