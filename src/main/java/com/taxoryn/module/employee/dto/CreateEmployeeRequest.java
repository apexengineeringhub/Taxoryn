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
@Schema(description = "Create Employee Record Payload")
public class CreateEmployeeRequest {

    @Schema(description = "Optional linked user account ID")
    private UUID userId;

    @Size(max = 50, message = "Employee code cannot exceed 50 characters")
    @Schema(description = "Optional employee code / number (auto-generated sequentially as EMP-0001 if omitted)", example = "EMP-0001")
    private String employeeCode;

    @Schema(description = "Optional employee number alias", example = "EMP-0001")
    private String employeeNumber;

    public String getEmployeeCode() {
        if (employeeCode != null && !employeeCode.isBlank()) {
            return employeeCode;
        }
        return employeeNumber;
    }

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

    @Pattern(regexp = "^(\\+?[0-9\\s-]{7,20})?$", message = "Invalid phone number format")
    @Schema(description = "Contact phone", example = "+919876543210")
    private String phone;

    @NotBlank(message = "Department is required")
    @Size(max = 100, message = "Department cannot exceed 100 characters")
    @Schema(description = "Department name", example = "Taxation")
    @Builder.Default
    private String department = "Taxation";

    @NotBlank(message = "Designation is required")
    @Size(max = 100, message = "Designation cannot exceed 100 characters")
    @Schema(description = "Designation / Role Title", example = "Senior Tax Associate")
    @Builder.Default
    private String designation = "Tax Associate";

    @Schema(description = "Date of joining", example = "2024-04-01")
    @Builder.Default
    private LocalDate joiningDate = LocalDate.now();

    @Schema(description = "Initial employment status", defaultValue = "INVITED")
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.INVITED;

    @Schema(description = "Reporting manager employee ID")
    private UUID managerId;

    @Schema(description = "Optional assigned role ID")
    private UUID roleId;

    @Schema(description = "Optional assigned role code (e.g. TAX_PROFESSIONAL, PRACTITIONER, MANAGER, STAFF)")
    private String roleCode;
}
