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

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get own employee profile", description = "Retrieves self-service employee profile for the authenticated user.")
    public ResponseEntity<ApiResponse<EmployeeDto>> getMyEmployeeProfile() {
        EmployeeDto dto = employeeService.getMyEmployeeProfile();
        return ResponseEntity.ok(ApiResponse.success("Employee profile retrieved successfully", dto));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update own employee profile", description = "Allows an authenticated employee to update their permitted personal contact fields (first name, last name, phone, avatar).")
    public ResponseEntity<ApiResponse<EmployeeDto>> updateMyEmployeeProfile(@Valid @RequestBody com.taxoryn.module.user.dto.UpdateUserProfileRequest request) {
        EmployeeDto updated = employeeService.updateMyEmployeeProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Employee profile updated successfully", updated));
    }

    @PostMapping(value = "/me/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload own employee avatar", description = "Uploads and scans an avatar photo for the authenticated employee.")
    public ResponseEntity<ApiResponse<EmployeeDto>> uploadMyEmployeeAvatar(
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        EmployeeDto updated = employeeService.uploadMyEmployeeAvatar(file);
        return ResponseEntity.ok(ApiResponse.success("Avatar uploaded successfully", updated));
    }

    @GetMapping("/me/avatar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Stream own employee avatar", description = "Streams the avatar binary image for the authenticated employee.")
    public ResponseEntity<byte[]> streamMyEmployeeAvatar() {
        byte[] bytes = employeeService.getMyEmployeeAvatarContent();
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.IMAGE_PNG)
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .header("X-Content-Type-Options", "nosniff")
                .body(bytes);
    }

    @GetMapping("/{employeeId}/avatar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Stream employee avatar", description = "Streams the avatar binary image for a specific employee in the tenant.")
    public ResponseEntity<byte[]> streamEmployeeAvatar(@PathVariable UUID employeeId) {
        byte[] bytes = employeeService.getEmployeeAvatarContent(employeeId);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.IMAGE_PNG)
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .header("X-Content-Type-Options", "nosniff")
                .body(bytes);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW') or hasAuthority('EMPLOYEE_READ') or hasAuthority('TASK_VIEW') or hasAuthority('TASK_CREATE') or hasAuthority('TASK_WRITE') or hasAuthority('TASK_UPDATE') or hasAuthority('CLIENT_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT')")
    @Operation(summary = "List & search employees with filters", description = "Retrieves paginated employees with keyword search (name, email, phone, code) and filtering by department, status, designation, or manager.")
    public ResponseEntity<ApiResponse<PagedResponse<EmployeeDto>>> getEmployees(@Valid @ModelAttribute EmployeeFilterRequest filterRequest) {
        PagedResponse<EmployeeDto> response = employeeService.getEmployees(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Employees retrieved successfully", response));
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW') or hasAuthority('EMPLOYEE_READ') or hasAuthority('TASK_VIEW') or hasAuthority('TASK_CREATE') or hasAuthority('TASK_WRITE') or hasAuthority('TASK_UPDATE') or hasAuthority('CLIENT_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT')")
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
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE') or hasAuthority('EMPLOYEE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER')")
    @Operation(summary = "Update employee", description = "Updates employee profile and reporting hierarchy within the authenticated tenant.")
    public ResponseEntity<ApiResponse<EmployeeDto>> updateEmployee(@PathVariable UUID employeeId, @Valid @RequestBody UpdateEmployeeRequest request) {
        EmployeeDto updated = employeeService.updateEmployee(employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", updated));
    }

    @PutMapping("/{employeeId}/role")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE') or hasAuthority('EMPLOYEE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER')")
    @Operation(summary = "Update employee practice role", description = "Updates the assigned practice RBAC role for the employee.")
    public ResponseEntity<ApiResponse<EmployeeDto>> updateEmployeeRole(@PathVariable UUID employeeId, @Valid @RequestBody com.taxoryn.module.employee.dto.UpdateEmployeeRoleRequest request) {
        EmployeeDto updated = employeeService.updateEmployeeRole(employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Employee practice role updated successfully", updated));
    }

    @PatchMapping("/{employeeId}/role")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE') or hasAuthority('EMPLOYEE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER')")
    @Operation(summary = "Patch employee practice role", description = "Updates the assigned practice RBAC role for the employee.")
    public ResponseEntity<ApiResponse<EmployeeDto>> patchEmployeeRole(@PathVariable UUID employeeId, @Valid @RequestBody com.taxoryn.module.employee.dto.UpdateEmployeeRoleRequest request) {
        EmployeeDto updated = employeeService.updateEmployeeRole(employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Employee practice role updated successfully", updated));
    }

    @PatchMapping("/{employeeId}/status")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE') or hasAuthority('EMPLOYEE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER')")
    @Operation(summary = "Update employment status (PATCH)", description = "Updates employee lifecycle status (INVITED, ACTIVE, SUSPENDED, INACTIVE, TERMINATED).")
    public ResponseEntity<ApiResponse<EmployeeDto>> updateEmployeeStatus(@PathVariable UUID employeeId, @Valid @RequestBody UpdateEmployeeStatusRequest request) {
        EmployeeDto updated = employeeService.updateEmployeeStatus(employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Employee status updated successfully to " + updated.getStatus(), updated));
    }

    @PutMapping("/{employeeId}/status")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE') or hasAuthority('EMPLOYEE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER')")
    @Operation(summary = "Update employment status (PUT)", description = "Updates employee lifecycle status (INVITED, ACTIVE, SUSPENDED, INACTIVE, TERMINATED).")
    public ResponseEntity<ApiResponse<EmployeeDto>> putEmployeeStatus(@PathVariable UUID employeeId, @Valid @RequestBody UpdateEmployeeStatusRequest request) {
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

    @PostMapping("/{employeeId}/resend-invitation")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE') or hasAuthority('EMPLOYEE_CREATE') or hasAuthority('EMPLOYEE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Resend employee invitation email", description = "Generates a fresh activation token and dispatches an invitation email to the employee.")
    public ResponseEntity<ApiResponse<Void>> resendInvitation(@PathVariable UUID employeeId) {
        employeeService.resendInvitation(employeeId);
        return ResponseEntity.ok(ApiResponse.success("Invitation email dispatched successfully", null));
    }
}
