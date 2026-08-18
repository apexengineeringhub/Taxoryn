package com.taxoryn.module.employee.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.employee.dto.CreateEmployeeRequest;
import com.taxoryn.module.employee.dto.EmployeeDto;
import com.taxoryn.module.employee.dto.EmployeeFilterRequest;
import com.taxoryn.module.employee.dto.EmployeeWorkloadDto;
import com.taxoryn.module.employee.dto.UpdateEmployeeRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeStatusRequest;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.mapper.EmployeeMapper;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    @Transactional
    public EmployeeDto createEmployee(CreateEmployeeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        String code = request.getEmployeeCode().trim();
        String email = request.getEmail().toLowerCase().trim();

        if (employeeRepository.existsByOrganizationIdAndEmployeeCode(organizationId, code)) {
            throw new DuplicateResourceException("Employee", "employeeCode", code);
        }

        if (employeeRepository.existsByOrganizationIdAndEmail(organizationId, email)) {
            throw new DuplicateResourceException("Employee", "email", email);
        }

        if (request.getUserId() != null) {
            userRepository.findByIdAndOrganizationId(request.getUserId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
        }

        if (request.getManagerId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getManagerId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager Employee", "id", request.getManagerId()));
        }

        EmployeeEntity employee = EmployeeEntity.builder()
                .userId(request.getUserId())
                .employeeCode(code)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName() != null ? request.getLastName().trim() : null)
                .email(email)
                .phone(request.getPhone())
                .department(request.getDepartment().trim())
                .designation(request.getDesignation().trim())
                .joiningDate(request.getJoiningDate() != null ? request.getJoiningDate() : LocalDate.now())
                .status(request.getStatus() != null ? request.getStatus() : EmployeeStatus.ACTIVE)
                .managerId(request.getManagerId())
                .build();
        employee.setOrganizationId(organizationId);

        EmployeeEntity saved = employeeRepository.save(employee);
        log.info("Created employee record: id={}, code={} for tenant={}", saved.getId(), saved.getEmployeeCode(), organizationId);
        return enrichDto(saved);
    }

    @Override
    @Transactional
    public EmployeeDto updateEmployee(UUID employeeId, UpdateEmployeeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        EmployeeEntity employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        String email = request.getEmail().toLowerCase().trim();
        if (!email.equalsIgnoreCase(employee.getEmail())
                && employeeRepository.existsByOrganizationIdAndEmail(organizationId, email)) {
            throw new DuplicateResourceException("Employee", "email", email);
        }

        if (request.getManagerId() != null) {
            if (request.getManagerId().equals(employeeId)) {
                throw new BusinessValidationException("An employee cannot be assigned as their own reporting manager");
            }
            employeeRepository.findByIdAndOrganizationId(request.getManagerId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager Employee", "id", request.getManagerId()));
        }

        if (request.getUserId() != null) {
            userRepository.findByIdAndOrganizationId(request.getUserId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
        }

        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName() != null ? request.getLastName().trim() : null);
        employee.setEmail(email);
        employee.setPhone(request.getPhone());
        employee.setDepartment(request.getDepartment().trim());
        employee.setDesignation(request.getDesignation().trim());
        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
        }
        if (request.getJoiningDate() != null) {
            employee.setJoiningDate(request.getJoiningDate());
        }
        employee.setUserId(request.getUserId());
        employee.setManagerId(request.getManagerId());

        EmployeeEntity saved = employeeRepository.save(employee);
        log.info("Updated employee: id={} for tenant={}", saved.getId(), organizationId);
        return enrichDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDto getEmployeeById(UUID employeeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        EmployeeEntity employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        return enrichDto(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EmployeeDto> getEmployees(EmployeeFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        Specification<EmployeeEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (StringUtils.hasText(filterRequest.getSearch())) {
                String searchPattern = "%" + filterRequest.getSearch().trim().toLowerCase() + "%";
                Predicate nameMatch = cb.or(
                        cb.like(cb.lower(root.get("firstName")), searchPattern),
                        cb.like(cb.lower(root.get("lastName")), searchPattern),
                        cb.like(cb.lower(root.get("email")), searchPattern),
                        cb.like(cb.lower(root.get("phone")), searchPattern),
                        cb.like(cb.lower(root.get("employeeCode")), searchPattern)
                );
                predicates.add(nameMatch);
            }

            if (StringUtils.hasText(filterRequest.getDepartment())) {
                predicates.add(cb.equal(cb.lower(root.get("department")), filterRequest.getDepartment().trim().toLowerCase()));
            }

            if (filterRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filterRequest.getStatus()));
            }

            if (StringUtils.hasText(filterRequest.getDesignation())) {
                predicates.add(cb.equal(cb.lower(root.get("designation")), filterRequest.getDesignation().trim().toLowerCase()));
            }

            if (filterRequest.getManagerId() != null) {
                predicates.add(cb.equal(root.get("managerId"), filterRequest.getManagerId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<EmployeeEntity> page = employeeRepository.findAll(spec, filterRequest.toPageable());
        return PagedResponse.of(page, this::enrichDto);
    }

    @Override
    @Transactional
    public EmployeeDto updateEmployeeStatus(UUID employeeId, UpdateEmployeeStatusRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        EmployeeEntity employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        employee.setStatus(request.getStatus());
        EmployeeEntity saved = employeeRepository.save(employee);
        log.info("Updated employee status: id={}, newStatus={} for tenant={}", employeeId, request.getStatus(), organizationId);
        return enrichDto(saved);
    }

    @Override
    @Transactional
    public void deleteEmployee(UUID employeeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        EmployeeEntity employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        employee.setStatus(EmployeeStatus.TERMINATED);
        employeeRepository.save(employee);
        log.info("Deactivated/Terminated employee: id={} for tenant={}", employeeId, organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeWorkloadDto getEmployeeWorkload(UUID employeeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        EmployeeEntity employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        Set<UUID> assigneeIds = new HashSet<>();
        assigneeIds.add(employee.getId());
        if (employee.getUserId() != null) {
            assigneeIds.add(employee.getUserId());
        }

        long totalAssigned = taskRepository.countAssignedTasks(organizationId, assigneeIds);
        long completed = taskRepository.countByStatuses(organizationId, assigneeIds, Set.of(TaskStatus.COMPLETED));
        Set<TaskStatus> pendingStatuses = Set.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.UNDER_REVIEW);
        long pending = taskRepository.countByStatuses(organizationId, assigneeIds, pendingStatuses);
        long overdue = taskRepository.countOverdueTasks(organizationId, assigneeIds, pendingStatuses, LocalDate.now());

        return EmployeeWorkloadDto.builder()
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getFullName())
                .totalAssignedTasks(totalAssigned)
                .pendingTasks(pending)
                .overdueTasks(overdue)
                .completedTasks(completed)
                .build();
    }

    private EmployeeDto enrichDto(EmployeeEntity employee) {
        EmployeeDto dto = employeeMapper.toDto(employee);
        dto.setFullName(employee.getFullName());

        if (employee.getManagerId() != null) {
            employeeRepository.findByIdAndOrganizationId(employee.getManagerId(), employee.getOrganizationId())
                    .ifPresent(manager -> dto.setManagerName(manager.getFullName()));
        }

        return dto;
    }
}
