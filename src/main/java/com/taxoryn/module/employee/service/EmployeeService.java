package com.taxoryn.module.employee.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.employee.dto.CreateEmployeeRequest;
import com.taxoryn.module.employee.dto.EmployeeDto;
import com.taxoryn.module.employee.dto.EmployeeFilterRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeRoleRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeStatusRequest;
import com.taxoryn.module.employee.dto.EmployeeWorkloadDto;

import java.util.UUID;

public interface EmployeeService {

    EmployeeDto createEmployee(CreateEmployeeRequest request);

    EmployeeDto updateEmployee(UUID employeeId, UpdateEmployeeRequest request);

    EmployeeDto getMyEmployeeProfile();

    EmployeeDto updateMyEmployeeProfile(com.taxoryn.module.user.dto.UpdateUserProfileRequest request);

    EmployeeDto uploadMyEmployeeAvatar(org.springframework.web.multipart.MultipartFile file);

    byte[] getEmployeeAvatarContent(UUID employeeId);

    byte[] getMyEmployeeAvatarContent();

    EmployeeDto getEmployeeById(UUID employeeId);

    PagedResponse<EmployeeDto> getEmployees(EmployeeFilterRequest filterRequest);

    EmployeeDto updateEmployeeStatus(UUID employeeId, UpdateEmployeeStatusRequest request);

    EmployeeDto updateEmployeeRole(UUID employeeId, UpdateEmployeeRoleRequest request);

    void deleteEmployee(UUID employeeId);

    void resendInvitation(UUID employeeId);

    EmployeeWorkloadDto getEmployeeWorkload(UUID employeeId);

    com.taxoryn.module.employee.dto.BulkEmployeeImportResultDto bulkCreateEmployees(java.util.List<CreateEmployeeRequest> requests);
}
