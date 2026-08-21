package com.taxoryn.module.employee.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
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

import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.taxoryn.core.security.PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final EmployeeMapper employeeMapper;
    private final com.taxoryn.module.audit.service.AuditService auditService;

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

        final UUID targetUserId;
        if (request.getUserId() != null) {
            userRepository.findByIdAndOrganizationId(request.getUserId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
            targetUserId = request.getUserId();
        } else {
            UserEntity user = provisionUserForEmployee(organizationId, email, request.getFirstName().trim(),
                    request.getLastName() != null ? request.getLastName().trim() : null,
                    request.getPhone(), request.getDesignation());
            targetUserId = user.getId();
        }

        if (request.getManagerId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getManagerId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager Employee", "id", request.getManagerId()));
        }

        EmployeeEntity employee = EmployeeEntity.builder()
                .userId(targetUserId)
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
        EmployeeDto result = enrichDto(saved);
        auditService.logEvent("EMPLOYEE_CREATED", "EMPLOYEE", saved.getId().toString(), null, result);
        return result;
    }

    private UserEntity provisionUserForEmployee(UUID organizationId, String email, String firstName, String lastName, String phone, String designation) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> {
                    String roleCode = "PRACTITIONER";
                    String desLower = designation != null ? designation.toLowerCase() : "";
                    if (desLower.contains("article") || desLower.contains("trainee") || desLower.contains("intern")) {
                        roleCode = "ARTICLE_ASSISTANT";
                    }

                    final String finalRoleCode = roleCode;
                    RoleEntity role = roleRepository.findByCodeAndIsSystemRoleTrue(finalRoleCode)
                            .or(() -> roleRepository.findByCodeAndIsSystemRoleTrue("PRACTITIONER"))
                            .or(() -> roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN"))
                            .orElse(null);

                    Set<RoleEntity> userRoles = new HashSet<>();
                    if (role != null) userRoles.add(role);

                    UserEntity user = UserEntity.builder()
                            .email(email.toLowerCase().trim())
                            .passwordHash(passwordEncoder.encode("Password123!"))
                            .firstName(firstName)
                            .lastName(lastName)
                            .phone(phone)
                            .status(UserEntity.UserStatus.ACTIVE)
                            .roles(userRoles)
                            .build();
                    user.setOrganizationId(organizationId);
                    UserEntity saved = userRepository.save(user);
                    log.info("Auto-provisioned UserEntity for employee: {} with role {}", email, finalRoleCode);
                    return saved;
                });
    }

    @Override
    @Transactional
    public EmployeeDto updateEmployee(UUID employeeId, UpdateEmployeeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        EmployeeEntity employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        EmployeeDto oldSnapshot = enrichDto(employee);

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
        EmployeeDto result = enrichDto(saved);
        auditService.logEvent("EMPLOYEE_UPDATED", "EMPLOYEE", saved.getId().toString(), oldSnapshot, result);
        return result;
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
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        Specification<EmployeeEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            // Scoping: Staff only see their department peers and managers
            if (scope.isStaff() && StringUtils.hasText(scope.getDepartment())) {
                predicates.add(cb.equal(cb.lower(root.get("department")), scope.getDepartment().trim().toLowerCase()));
            }

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

        EmployeeStatus oldStatus = employee.getStatus();
        employee.setStatus(request.getStatus());
        EmployeeEntity saved = employeeRepository.save(employee);
        log.info("Updated employee status: id={}, newStatus={} for tenant={}", employeeId, request.getStatus(), organizationId);
        EmployeeDto result = enrichDto(saved);
        auditService.logEvent("EMPLOYEE_STATUS_UPDATED", "EMPLOYEE", employeeId.toString(), oldStatus != null ? oldStatus.name() : null, request.getStatus().name());
        return result;
    }

    @Override
    @Transactional
    public void deleteEmployee(UUID employeeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        EmployeeEntity employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        EmployeeStatus oldStatus = employee.getStatus();
        employee.setStatus(EmployeeStatus.TERMINATED);
        employeeRepository.save(employee);
        log.info("Deactivated/Terminated employee: id={} for tenant={}", employeeId, organizationId);
        auditService.logEvent("EMPLOYEE_DELETED", "EMPLOYEE", employeeId.toString(), oldStatus != null ? oldStatus.name() : null, EmployeeStatus.TERMINATED.name());
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
        if (employee == null) return null;
        EmployeeDto dto = employeeMapper.toDto(employee);
        if (dto == null) {
            return null;
        }
        dto.setFullName(employee.getFullName());

        if (employee.getManagerId() != null) {
            employeeRepository.findByIdAndOrganizationId(employee.getManagerId(), employee.getOrganizationId())
                    .ifPresent(manager -> dto.setManagerName(manager.getFullName()));
        }

        return dto;
    }

    @Override
    @Transactional
    public com.taxoryn.module.employee.dto.BulkEmployeeImportResultDto bulkCreateEmployees(java.util.List<CreateEmployeeRequest> requests) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        com.taxoryn.module.employee.dto.BulkEmployeeImportResultDto result = com.taxoryn.module.employee.dto.BulkEmployeeImportResultDto.builder()
                .totalProcessed(requests != null ? requests.size() : 0)
                .build();

        if (requests == null || requests.isEmpty()) {
            return result;
        }

        int row = 1;
        for (CreateEmployeeRequest req : requests) {
            row++;
            try {
                String code = req.getEmployeeCode() != null ? req.getEmployeeCode().trim() : ("EMP-" + (System.currentTimeMillis() % 100000));
                String email = req.getEmail() != null ? req.getEmail().toLowerCase().trim() : null;

                if (email == null || email.isBlank()) {
                    result.getErrors().add("Row " + row + ": Email is required");
                    result.setTotalFailed(result.getTotalFailed() + 1);
                    continue;
                }

                if (employeeRepository.existsByOrganizationIdAndEmployeeCode(organizationId, code)) {
                    result.getErrors().add("Row " + row + " (Code: " + code + "): Employee code already exists, skipped");
                    result.setTotalSkipped(result.getTotalSkipped() + 1);
                    continue;
                }

                if (employeeRepository.existsByOrganizationIdAndEmail(organizationId, email)) {
                    result.getErrors().add("Row " + row + " (Email: " + email + "): Employee email already exists, skipped");
                    result.setTotalSkipped(result.getTotalSkipped() + 1);
                    continue;
                }

                final UUID targetUserId;
                if (req.getUserId() != null) {
                    userRepository.findByIdAndOrganizationId(req.getUserId(), organizationId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", req.getUserId()));
                    targetUserId = req.getUserId();
                } else {
                    String firstName = req.getFirstName() != null ? req.getFirstName().trim() : "Staff";
                    String lastName = req.getLastName() != null ? req.getLastName().trim() : null;
                    String designation = req.getDesignation() != null ? req.getDesignation().trim() : "Tax Associate";
                    UserEntity user = provisionUserForEmployee(organizationId, email, firstName, lastName, req.getPhone(), designation);
                    targetUserId = user.getId();
                }

                EmployeeEntity employee = EmployeeEntity.builder()
                        .userId(targetUserId)
                        .employeeCode(code)
                        .firstName(req.getFirstName() != null ? req.getFirstName().trim() : "Staff")
                        .lastName(req.getLastName() != null ? req.getLastName().trim() : null)
                        .email(email)
                        .phone(req.getPhone())
                        .department(req.getDepartment() != null ? req.getDepartment().trim() : "Taxation")
                        .designation(req.getDesignation() != null ? req.getDesignation().trim() : "Tax Associate")
                        .joiningDate(req.getJoiningDate() != null ? req.getJoiningDate() : LocalDate.now())
                        .status(req.getStatus() != null ? req.getStatus() : EmployeeStatus.ACTIVE)
                        .managerId(req.getManagerId())
                        .build();
                employee.setOrganizationId(organizationId);

                EmployeeEntity saved = employeeRepository.save(employee);
                result.getCreatedEmployees().add(enrichDto(saved));
                result.setTotalCreated(result.getTotalCreated() + 1);

                auditService.logEvent(
                        "CREATE_EMPLOYEE",
                        "EMPLOYEE",
                        saved.getId().toString(),
                        null,
                        "Bulk onboarded employee: " + saved.getFullName() + " (" + saved.getEmployeeCode() + ")"
                );
            } catch (Exception ex) {
                result.getErrors().add("Row " + row + " (" + req.getEmail() + "): " + ex.getMessage());
                result.setTotalFailed(result.getTotalFailed() + 1);
            }
        }

        log.info("Bulk imported employees for orgId={}: {} created, {} skipped, {} failed",
                organizationId, result.getTotalCreated(), result.getTotalSkipped(), result.getTotalFailed());

        return result;
    }
}
