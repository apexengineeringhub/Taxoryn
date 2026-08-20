package com.taxoryn.module.employee.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.employee.dto.CreateEmployeeRequest;
import com.taxoryn.module.employee.dto.EmployeeDto;
import com.taxoryn.module.employee.dto.EmployeeFilterRequest;
import com.taxoryn.module.employee.dto.EmployeeWorkloadDto;
import com.taxoryn.module.employee.dto.UpdateEmployeeRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeStatusRequest;
import com.taxoryn.module.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/employees", "/api/employees"})
@RequiredArgsConstructor
@Tag(name = "Employee Management", description = "Endpoints for managing firm staff, reporting hierarchy, and workload tracking")
@SecurityRequirement(name = "BearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW') or hasAuthority('EMPLOYEE_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List & search employees with filters", description = "Retrieves paginated employees with keyword search (name, email, phone, code) and filtering by department, status, designation, or manager.")
    public ResponseEntity<ApiResponse<PagedResponse<EmployeeDto>>> getEmployees(@Valid @ModelAttribute EmployeeFilterRequest filterRequest) {
        PagedResponse<EmployeeDto> response = employeeService.getEmployees(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Employees retrieved successfully", response));
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW') or hasAuthority('EMPLOYEE_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get employee by ID", description = "Retrieves complete employee profile within the authenticated tenant.")
    public ResponseEntity<ApiResponse<EmployeeDto>> getEmployeeById(@PathVariable UUID employeeId) {
        EmployeeDto dto = employeeService.getEmployeeById(employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employee retrieved successfully", dto));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE') or hasAuthority('EMPLOYEE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create employee", description = "Onboards a new employee record within the authenticated tenant organization.")
    public ResponseEntity<ApiResponse<EmployeeDto>> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeDto created = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Employee created successfully", created));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE') or hasAuthority('EMPLOYEE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Bulk onboard practice employees and practitioners", description = "Onboards multiple staff members in a batch from spreadsheets.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.employee.dto.BulkEmployeeImportResultDto>> bulkCreateEmployees(@RequestBody java.util.List<CreateEmployeeRequest> requests) {
        com.taxoryn.module.employee.dto.BulkEmployeeImportResultDto result = employeeService.bulkCreateEmployees(requests);
        return ResponseEntity.ok(ApiResponse.success("Bulk employee onboarding completed", result));
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE') or hasAuthority('EMPLOYEE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update employee", description = "Updates employee profile and reporting hierarchy within the authenticated tenant.")
    public ResponseEntity<ApiResponse<EmployeeDto>> updateEmployee(@PathVariable UUID employeeId, @Valid @RequestBody UpdateEmployeeRequest request) {
        EmployeeDto updated = employeeService.updateEmployee(employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", updated));
    }

    @PatchMapping("/{employeeId}/status")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE') or hasAuthority('EMPLOYEE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update employment status", description = "Updates employee lifecycle status (ACTIVE, INACTIVE, ON_LEAVE, RESIGNED, TERMINATED).")
    public ResponseEntity<ApiResponse<EmployeeDto>> updateEmployeeStatus(@PathVariable UUID employeeId, @Valid @RequestBody UpdateEmployeeStatusRequest request) {
        EmployeeDto updated = employeeService.updateEmployeeStatus(employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Employee status updated successfully to " + updated.getStatus(), updated));
    }

    @DeleteMapping("/{employeeId}")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE') or hasAuthority('EMPLOYEE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Deactivate / Terminate employee", description = "Marks employee record as terminated within the authenticated tenant.")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable UUID employeeId) {
        employeeService.deleteEmployee(employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employee deactivated/terminated successfully", null));
    }

    @GetMapping("/{employeeId}/workload")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW') or hasAuthority('EMPLOYEE_READ') or hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get employee workload metrics", description = "Computes task workload metrics (total assigned, pending, overdue, completed tasks) for the employee.")
    public ResponseEntity<ApiResponse<EmployeeWorkloadDto>> getEmployeeWorkload(@PathVariable UUID employeeId) {
        EmployeeWorkloadDto workload = employeeService.getEmployeeWorkload(employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employee workload retrieved successfully", workload));
    }
}
