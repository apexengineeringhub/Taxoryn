package com.taxoryn.module.employee.dto;

import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Employee Record Payload")
public class UpdateEmployeeRequest {

    @Schema(description = "Optional linked user account ID")
    private UUID userId;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    @Schema(description = "First name", example = "Rohan")
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    @Schema(description = "Last name", example = "Deshmukh")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Official email address", example = "rohan.d@taxpractice.com")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    @Schema(description = "Contact phone", example = "+919876543210")
    private String phone;

    @NotBlank(message = "Department is required")
    @Size(max = 100, message = "Department cannot exceed 100 characters")
    @Schema(description = "Department name", example = "Taxation & Audit")
    private String department;

    @NotBlank(message = "Designation is required")
    @Size(max = 100, message = "Designation cannot exceed 100 characters")
    @Schema(description = "Designation / Role Title", example = "Principal Tax Consultant")
    private String designation;

    @Schema(description = "Employment status")
    private EmployeeStatus status;

    @Schema(description = "Date of joining", example = "2024-04-01")
    private LocalDate joiningDate;

    @Schema(description = "Reporting manager employee ID")
    private UUID managerId;
}
