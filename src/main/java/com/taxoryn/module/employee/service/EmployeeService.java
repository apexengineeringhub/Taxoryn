package com.taxoryn.module.employee.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.employee.dto.CreateEmployeeRequest;
import com.taxoryn.module.employee.dto.EmployeeDto;
import com.taxoryn.module.employee.dto.EmployeeFilterRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeStatusRequest;
import com.taxoryn.module.employee.dto.EmployeeWorkloadDto;

import java.util.UUID;

public interface EmployeeService {

    EmployeeDto createEmployee(CreateEmployeeRequest request);

    EmployeeDto updateEmployee(UUID employeeId, UpdateEmployeeRequest request);

    EmployeeDto getEmployeeById(UUID employeeId);

    PagedResponse<EmployeeDto> getEmployees(EmployeeFilterRequest filterRequest);

    EmployeeDto updateEmployeeStatus(UUID employeeId, UpdateEmployeeStatusRequest request);

    void deleteEmployee(UUID employeeId);

    EmployeeWorkloadDto getEmployeeWorkload(UUID employeeId);
}
