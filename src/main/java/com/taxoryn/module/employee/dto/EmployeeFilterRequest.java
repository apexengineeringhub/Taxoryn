package com.taxoryn.module.employee.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Employee Search, Filter, and Pagination Parameters")
public class EmployeeFilterRequest extends PageRequestDto {

    @Schema(description = "Search term across name, email, phone, and employee code", example = "Rohan")
    private String search;

    @Schema(description = "Filter by department", example = "Taxation")
    private String department;

    @Schema(description = "Filter by employment status", example = "ACTIVE")
    private EmployeeStatus status;

    @Schema(description = "Filter by designation", example = "Senior Tax Associate")
    private String designation;

    @Schema(description = "Filter by reporting manager ID")
    private UUID managerId;
}
