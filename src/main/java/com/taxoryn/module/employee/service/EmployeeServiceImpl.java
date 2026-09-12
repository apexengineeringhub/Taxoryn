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

import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.authentication.entity.OrganizationActivationTokenEntity;
import com.taxoryn.module.authentication.repository.OrganizationActivationTokenRepository;
import com.taxoryn.module.notification.email.service.EmailNotificationService;
import com.taxoryn.core.security.PasswordSecurityUtils;
import org.springframework.beans.factory.annotation.Value;

import com.taxoryn.module.employee.dto.UpdateEmployeeRoleRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationActivationTokenRepository organizationActivationTokenRepository;
    private final com.taxoryn.module.authentication.repository.RefreshTokenRepository refreshTokenRepository;
    private final EmailNotificationService emailNotificationService;
    private final PasswordEncoder passwordEncoder;
    private final com.taxoryn.core.security.PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final EmployeeMapper employeeMapper;
    private final com.taxoryn.module.audit.service.AuditService auditService;
    private final OrganizationEmployeeNumberGenerator employeeNumberGenerator;

    @Value("${taxoryn.auth.activation-url:${taxoryn.frontend.activation-url:${taxoryn.auth.activation-base-url:${taxoryn.mail.activation-url:${TAXORYN_ACTIVATION_URL:${taxoryn.frontend-url:${app.frontend-url:${TAXORYN_FRONTEND_URL:${FRONTEND_URL:http://localhost:5173}}}}/activate}}}}}")
    private String activationBaseUrl = "http://localhost:5173/activate";

    @Value("${taxoryn.auth.activation.expiration-hours:24}")
    private long activationExpirationHours = 24L;

    @Override
    @Transactional
    public EmployeeDto createEmployee(CreateEmployeeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        String code = request.getEmployeeCode() != null ? request.getEmployeeCode().trim() : null;
        if (!StringUtils.hasText(code)) {
            code = employeeNumberGenerator.generateNextEmployeeCode(organizationId);
        } else if (employeeRepository.existsByOrganizationIdAndEmployeeCode(organizationId, code)) {
            throw new DuplicateResourceException("Employee", "employeeCode", code);
        }

        String email = request.getEmail().toLowerCase().trim();

        if (employeeRepository.existsByOrganizationIdAndEmail(organizationId, email)) {
            throw new DuplicateResourceException("Employee", "email", email);
        }

        final UUID targetUserId;
        if (request.getUserId() != null) {
            UserEntity targetUser = userRepository.findByIdAndOrganizationId(request.getUserId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
            if (targetUser.getClientId() != null) {
                throw new BusinessValidationException("Cannot link a client portal user to an employee account");
            }
            if (employeeRepository.findByOrganizationIdAndUserId(organizationId, targetUser.getId()).isPresent()) {
                throw new DuplicateResourceException("Employee", "userId", targetUser.getId());
            }
            targetUserId = request.getUserId();
        } else {
            UserEntity user = provisionUserForEmployee(organizationId, email, request.getFirstName().trim(),
                    request.getLastName() != null ? request.getLastName().trim() : null,
                    request.getPhone(), request.getDesignation(), request.getRoleCode(), request.getRoleId());
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
                .status(request.getStatus() != null ? request.getStatus() : EmployeeStatus.INVITED)
                .managerId(request.getManagerId())
                .build();
        employee.setOrganizationId(organizationId);

        EmployeeEntity saved = employeeRepository.save(employee);
        log.info("Created employee record: id={}, code={} for tenant={}", saved.getId(), saved.getEmployeeCode(), organizationId);

        UserEntity user = userRepository.findByIdAndOrganizationId(targetUserId, organizationId).orElse(null);
        if (user != null) {
            sendEmployeeInvitation(saved, user, organizationId);
        }

        EmployeeDto result = enrichDto(saved);
        auditService.logEvent("EMPLOYEE_CREATED", "EMPLOYEE", saved.getId().toString(), null, result);
        return result;
    }

    private UserEntity provisionUserForEmployee(UUID organizationId, String email, String firstName, String lastName, String phone, String designation) {
        return provisionUserForEmployee(organizationId, email, firstName, lastName, phone, designation, null, null);
    }

    private UserEntity provisionUserForEmployee(UUID organizationId, String email, String firstName, String lastName, String phone, String designation, String requestedRoleCode, UUID requestedRoleId) {
        RoleEntity role = null;
        if (requestedRoleId != null) {
            role = roleRepository.findById(requestedRoleId).orElse(null);
        } else if (requestedRoleCode != null && !requestedRoleCode.isBlank()) {
            String clean = requestedRoleCode.trim().toUpperCase();
            if (clean.startsWith("ROLE_")) {
                clean = clean.substring(5);
            }
            final String cleanCode = clean;
            if (SecurityUtils.isPlatformRole(cleanCode) && !SecurityUtils.isTaxorynSuperAdmin()) {
                throw new com.taxoryn.core.exception.ForbiddenException(
                        "Privilege escalation denied: Platform role '" + cleanCode + "' cannot be assigned by tenant users"
                );
            }
            if (SecurityUtils.isClientRole(cleanCode) && !SecurityUtils.isTaxorynSuperAdmin()) {
                throw new com.taxoryn.core.exception.ForbiddenException(
                        "Invalid role assignment: Client role '" + cleanCode + "' cannot be assigned as a practice employee role"
                );
            }
            role = roleRepository.findByCodeAndOrganizationId(cleanCode, organizationId)
                    .or(() -> roleRepository.findByCodeAndIsSystemRoleTrue(cleanCode))
                    .orElse(null);
        }

        // SECURITY FIX #6: RBAC Role is NEVER inferred from employee designation or job title.
        // If no explicit role was requested, default strictly to the base staff role (TAX_ASSOCIATE / STAFF).
        if (role == null) {
            role = roleRepository.findByCodeAndOrganizationId("TAX_ASSOCIATE", organizationId)
                    .or(() -> roleRepository.findByCodeAndIsSystemRoleTrue("TAX_ASSOCIATE"))
                    .or(() -> roleRepository.findByCodeAndIsSystemRoleTrue("STAFF"))
                    .orElse(null);
        }

        final RoleEntity resolvedRole = role;

        Optional<UserEntity> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            UserEntity u = existing.get();
            // Tenant Isolation Rule 1: Never attach or reassign a user from another organization or platform
            if (u.getOrganizationId() == null || !u.getOrganizationId().equals(organizationId)) {
                log.warn("Cross-tenant employee provisioning blocked: Email {} belongs to foreign organization/user {}", email, u.getOrganizationId());
                throw new DuplicateResourceException("The email address is already registered and cannot be used for this employee account.");
            }

            // Tenant Isolation Rule 2: Same-organization user handling
            if (u.getClientId() != null) {
                log.warn("Client user employee provisioning blocked: Email {} in tenant {} is assigned to client {}", email, organizationId, u.getClientId());
                throw new DuplicateResourceException("The email address belongs to an existing client user and cannot be attached to an employee account.");
            }

            if (employeeRepository.findByOrganizationIdAndUserId(organizationId, u.getId()).isPresent()) {
                log.warn("Duplicate employee provisioning blocked: User {} ({}) is already linked to an employee in tenant {}", u.getId(), email, organizationId);
                throw new DuplicateResourceException("An employee record already exists for this user account.");
            }

            // EDGE CASE: If email belongs to an existing internal user in the same organization,
            // return a clear conflict. Do not silently convert or reassign that user.
            log.warn("Internal user conflict: User {} ({}) already exists in tenant {}. Explicit userId required.", u.getId(), email, organizationId);
            throw new DuplicateResourceException("A user with email '" + email + "' already exists in this organization. To create an employee for an existing user, specify userId explicitly.");
        }

        Set<RoleEntity> userRoles = new HashSet<>();
        if (resolvedRole != null) userRoles.add(resolvedRole);

        UserEntity user = UserEntity.builder()
                .email(email.toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(com.taxoryn.core.security.PasswordSecurityUtils.generateSecureTemporaryPassword()))
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .status(UserEntity.UserStatus.INVITED)
                .roles(userRoles)
                .build();
        user.setOrganizationId(organizationId);
        UserEntity saved = userRepository.save(user);
        log.info("Auto-provisioned UserEntity for employee: {} with role {} in INVITED status", email, resolvedRole != null ? resolvedRole.getCode() : "none");
        return saved;
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
            UserEntity targetUser = userRepository.findByIdAndOrganizationId(request.getUserId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
            if (targetUser.getClientId() != null) {
                throw new BusinessValidationException("Cannot link a client portal user to an employee account");
            }
            Optional<EmployeeEntity> existingEmpForUser = employeeRepository.findByOrganizationIdAndUserId(organizationId, request.getUserId());
            if (existingEmpForUser.isPresent() && !existingEmpForUser.get().getId().equals(employeeId)) {
                throw new DuplicateResourceException("Employee", "userId", request.getUserId());
            }
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

        // Handle Role Update
        if (request.getRoleCode() != null || request.getRoleId() != null) {
            applyEmployeeRoleChange(employee, request.getRoleCode(), request.getRoleId(), organizationId);
        }

        EmployeeEntity saved = employeeRepository.save(employee);
        log.info("Updated employee: id={} for tenant={}", saved.getId(), organizationId);
        EmployeeDto result = enrichDto(saved);
        auditService.logEvent("EMPLOYEE_UPDATED", "EMPLOYEE", saved.getId().toString(), oldSnapshot, result);
        return result;
    }

    @Override
    @Transactional
    public EmployeeDto updateEmployeeRole(UUID employeeId, com.taxoryn.module.employee.dto.UpdateEmployeeRoleRequest request) {
        if (request == null || (!StringUtils.hasText(request.getRoleCode()) && request.getRoleId() == null)) {
            throw new BusinessValidationException("Target role code or role ID must be provided");
        }

        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        EmployeeEntity employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        EmployeeDto oldSnapshot = enrichDto(employee);

        applyEmployeeRoleChange(employee, request.getRoleCode(), request.getRoleId(), organizationId);

        EmployeeEntity saved = employeeRepository.save(employee);
        log.info("Updated role for employee: id={} for tenant={}", saved.getId(), organizationId);
        EmployeeDto result = enrichDto(saved);
        auditService.logEvent("EMPLOYEE_ROLE_UPDATED", "EMPLOYEE", saved.getId().toString(), oldSnapshot, result);
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
        if (request == null || request.getStatus() == null) {
            throw new BusinessValidationException("Target employment status is required");
        }

        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        EmployeeEntity employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        // 1. Block self-status update (Practice admin or staff cannot suspend / deactivate themselves)
        if (employee.getUserId() != null && employee.getUserId().equals(currentUserId)) {
            throw new BusinessValidationException("You cannot modify your own employee account status");
        }
        if (employee.getEmail() != null) {
            String currentUserEmail = SecurityUtils.getCurrentUser().map(com.taxoryn.core.security.SecurityUser::getUsername).orElse(null);
            if (employee.getEmail().equalsIgnoreCase(currentUserEmail)) {
                throw new BusinessValidationException("You cannot modify your own employee account status");
            }
        }

        EmployeeStatus oldStatus = employee.getStatus();
        EmployeeStatus newStatus = request.getStatus();

        if (oldStatus == newStatus) {
            return enrichDto(employee);
        }

        // 2. Resolve linked UserEntity strictly within tenant
        UserEntity user = null;
        if (employee.getUserId() != null) {
            user = userRepository.findByIdAndOrganizationId(employee.getUserId(), organizationId).orElse(null);
        }
        if (user == null && employee.getEmail() != null) {
            user = userRepository.findByOrganizationIdAndEmailIgnoreCase(organizationId, employee.getEmail()).orElse(null);
        }

        // 3. Last Active Practice Admin Lockout Protection
        boolean isDeactivatingOrSuspending = (newStatus == EmployeeStatus.SUSPENDED || newStatus == EmployeeStatus.INACTIVE || newStatus == EmployeeStatus.TERMINATED);
        if (isDeactivatingOrSuspending && user != null && user.getStatus() == UserEntity.UserStatus.ACTIVE) {
            boolean hasAdminRole = user.getRoles() != null && user.getRoles().stream()
                    .anyMatch(r -> "ORG_ADMIN".equals(r.getCode()) || "PRACTICE_ADMIN".equals(r.getCode()) || "PRACTICE_OWNER".equals(r.getCode()));
            if (hasAdminRole) {
                long activeAdminCount = userRepository.countActiveOrgAdmins(organizationId);
                if (activeAdminCount <= 1) {
                    throw new BusinessValidationException("Cannot suspend or deactivate the last remaining active Organization Administrator");
                }
            }
        }

        // 4. Update Employee Status
        employee.setStatus(newStatus);
        EmployeeEntity saved = employeeRepository.save(employee);

        // 5. Synchronize UserEntity status and session tokens
        if (user != null) {
            if (newStatus == EmployeeStatus.ACTIVE) {
                user.setStatus(UserEntity.UserStatus.ACTIVE);
                userRepository.save(user);
            } else if (newStatus == EmployeeStatus.SUSPENDED) {
                user.setStatus(UserEntity.UserStatus.SUSPENDED);
                userRepository.save(user);
                refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now(), "ACCOUNT_SUSPENDED");
                log.info("Revoked all active refresh sessions for suspended employee user: {}", user.getId());
            } else if (newStatus == EmployeeStatus.INACTIVE || newStatus == EmployeeStatus.TERMINATED || newStatus == EmployeeStatus.RESIGNED) {
                user.setStatus(UserEntity.UserStatus.INACTIVE);
                userRepository.save(user);
                refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now(), "ACCOUNT_DEACTIVATED");
                log.info("Revoked all active refresh sessions for deactivated employee user: {}", user.getId());
            } else if (newStatus == EmployeeStatus.INVITED) {
                user.setStatus(UserEntity.UserStatus.INVITED);
                userRepository.save(user);
            }
        }

        log.info("Updated employee status: id={}, oldStatus={}, newStatus={} for tenant={}", employeeId, oldStatus, newStatus, organizationId);
        EmployeeDto result = enrichDto(saved);
        auditService.logEvent(organizationId, currentUserId, "EMPLOYEE_STATUS_UPDATED", "EMPLOYEE", employeeId.toString(), oldStatus != null ? oldStatus.name() : null, newStatus.name());
        return result;
    }

    @Override
    @Transactional
    public void deleteEmployee(UUID employeeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        EmployeeEntity employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        if (employee.getUserId() != null && employee.getUserId().equals(currentUserId)) {
            throw new BusinessValidationException("You cannot terminate your own employee account");
        }

        UserEntity user = null;
        if (employee.getUserId() != null) {
            user = userRepository.findByIdAndOrganizationId(employee.getUserId(), organizationId).orElse(null);
        }
        if (user == null && employee.getEmail() != null) {
            user = userRepository.findByOrganizationIdAndEmailIgnoreCase(organizationId, employee.getEmail()).orElse(null);
        }

        if (user != null && user.getStatus() == UserEntity.UserStatus.ACTIVE) {
            boolean hasAdminRole = user.getRoles() != null && user.getRoles().stream()
                    .anyMatch(r -> "ORG_ADMIN".equals(r.getCode()) || "PRACTICE_ADMIN".equals(r.getCode()) || "PRACTICE_OWNER".equals(r.getCode()));
            if (hasAdminRole) {
                long activeAdminCount = userRepository.countActiveOrgAdmins(organizationId);
                if (activeAdminCount <= 1) {
                    throw new BusinessValidationException("Cannot terminate the last remaining active Organization Administrator");
                }
            }
        }

        EmployeeStatus oldStatus = employee.getStatus();
        employee.setStatus(EmployeeStatus.TERMINATED);
        employeeRepository.save(employee);

        if (user != null) {
            user.setStatus(UserEntity.UserStatus.INACTIVE);
            userRepository.save(user);
            refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now(), "ACCOUNT_TERMINATED");
        }

        log.info("Deactivated/Terminated employee: id={} for tenant={}", employeeId, organizationId);
        auditService.logEvent(organizationId, currentUserId, "EMPLOYEE_DELETED", "EMPLOYEE", employeeId.toString(), oldStatus != null ? oldStatus.name() : null, EmployeeStatus.TERMINATED.name());
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
        dto.setEmployeeCode(employee.getEmployeeCode());
        dto.setEmployeeNumber(employee.getEmployeeCode());
        dto.setFullName(employee.getFullName());

        if (employee.getManagerId() != null) {
            employeeRepository.findByIdAndOrganizationId(employee.getManagerId(), employee.getOrganizationId())
                    .ifPresent(manager -> dto.setManagerName(manager.getFullName()));
        }

        // Populate Role details from linked UserEntity strictly within tenant
        UserEntity user = null;
        if (employee.getUserId() != null && employee.getOrganizationId() != null) {
            user = userRepository.findByIdAndOrganizationId(employee.getUserId(), employee.getOrganizationId()).orElse(null);
        }
        if (user == null && employee.getEmail() != null && employee.getOrganizationId() != null) {
            user = userRepository.findByOrganizationIdAndEmailIgnoreCase(employee.getOrganizationId(), employee.getEmail()).orElse(null);
        }

        if (user != null && user.getRoles() != null && !user.getRoles().isEmpty()) {
            RoleEntity primaryRole = user.getRoles().iterator().next();
            // Prefer an admin role if user has multiple roles
            for (RoleEntity r : user.getRoles()) {
                if ("ORG_ADMIN".equals(r.getCode()) || "PRACTICE_ADMIN".equals(r.getCode()) || "PRACTICE_OWNER".equals(r.getCode()) || "PARTNER".equals(r.getCode())) {
                    primaryRole = r;
                    break;
                }
            }
            dto.setRoleId(primaryRole.getId());
            dto.setRoleCode(primaryRole.getCode());
            dto.setRoleName(primaryRole.getName());
            if (dto.getUserId() == null) {
                dto.setUserId(user.getId());
            }
        } else {
            // SECURITY FIX #6: When no RBAC role is assigned, do NOT infer security roles from designation.
            dto.setRoleId(null);
            dto.setRoleCode(null);
            dto.setRoleName(null);
        }

        return dto;
    }

    private void applyEmployeeRoleChange(EmployeeEntity employee, String requestedRoleCode, UUID requestedRoleId, UUID organizationId) {
        // Resolve target role first
        RoleEntity targetRole = null;
        if (requestedRoleId != null) {
            targetRole = roleRepository.findById(requestedRoleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "id", requestedRoleId));
        } else if (requestedRoleCode != null && !requestedRoleCode.isBlank()) {
            String cleanCode = requestedRoleCode.trim().toUpperCase();
            if (cleanCode.startsWith("ROLE_")) {
                cleanCode = cleanCode.substring(5);
            }
            final String codeToFind = cleanCode;

            // 1. HARD SECURITY RULE: Platform role assignment rejection
            if (SecurityUtils.isPlatformRole(codeToFind) && !SecurityUtils.isTaxorynSuperAdmin()) {
                throw new com.taxoryn.core.exception.ForbiddenException(
                        "Privilege escalation denied: Platform role '" + codeToFind + "' cannot be assigned by tenant users"
                );
            }

            // 2. Reject client roles for practice staff
            if (SecurityUtils.isClientRole(codeToFind) && !SecurityUtils.isTaxorynSuperAdmin()) {
                throw new com.taxoryn.core.exception.ForbiddenException(
                        "Invalid role assignment: Client role '" + codeToFind + "' cannot be assigned as a practice employee role"
                );
            }

            targetRole = roleRepository.findByCodeAndOrganizationId(codeToFind, organizationId)
                    .or(() -> roleRepository.findByCodeAndIsSystemRoleTrue(codeToFind))
                    .orElseThrow(() -> new BusinessValidationException("Role '" + codeToFind + "' is not a valid practice role"));
        }

        if (targetRole == null) {
            throw new BusinessValidationException("Target role must be specified");
        }

        // Check if custom role belongs to another organization
        if (!targetRole.isSystemRole() && (targetRole.getOrganizationId() == null || !targetRole.getOrganizationId().equals(organizationId))) {
            throw new com.taxoryn.core.exception.ForbiddenException("Access denied: Custom role belongs to another organization");
        }

        // Check if platform role
        if (SecurityUtils.isPlatformRole(targetRole.getCode()) && !SecurityUtils.isTaxorynSuperAdmin()) {
            throw new com.taxoryn.core.exception.ForbiddenException(
                    "Privilege escalation denied: Platform role '" + targetRole.getCode() + "' cannot be assigned by tenant users"
            );
        }

        // Resolve linked user strictly within tenant
        UserEntity user = null;
        if (employee.getUserId() != null) {
            user = userRepository.findByIdAndOrganizationId(employee.getUserId(), organizationId).orElse(null);
        }
        if (user == null && employee.getEmail() != null) {
            user = userRepository.findByOrganizationIdAndEmailIgnoreCase(organizationId, employee.getEmail()).orElse(null);
        }

        if (user == null) {
            // Provision user if not yet linked
            user = provisionUserForEmployee(organizationId, employee.getEmail(), employee.getFirstName(), employee.getLastName(), employee.getPhone(), employee.getDesignation(), requestedRoleCode, requestedRoleId);
            employee.setUserId(user.getId());
        } else {
            if (employee.getUserId() == null) {
                employee.setUserId(user.getId());
            }
        }

        // Privilege escalation and delegation validation
        SecurityUtils.validateRoleDelegation(Set.of(targetRole.getCode()), user.getId());

        // Lockout prevention: Check if demoting sole active practice admin
        boolean currentlyIsOrgAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "ORG_ADMIN".equals(r.getCode()) || "PRACTICE_ADMIN".equals(r.getCode()) || "PRACTICE_OWNER".equals(r.getCode()));
        boolean willBeOrgAdmin = "ORG_ADMIN".equals(targetRole.getCode()) || "PRACTICE_ADMIN".equals(targetRole.getCode()) || "PRACTICE_OWNER".equals(targetRole.getCode());
        if (currentlyIsOrgAdmin && !willBeOrgAdmin) {
            long adminCount = userRepository.countActiveOrgAdmins(organizationId);
            if (adminCount <= 1) {
                throw new BusinessValidationException("Cannot demote the last remaining active Organization Administrator");
            }
        }

        Set<String> oldRoles = user.getRoles() != null
                ? user.getRoles().stream().map(RoleEntity::getCode).collect(Collectors.toSet())
                : Set.of();
        user.setRoles(new HashSet<>(List.of(targetRole)));
        userRepository.save(user);

        log.info("Updated role for employee {} (user {}) to {} in org {}", employee.getId(), user.getId(), targetRole.getCode(), organizationId);
        auditService.logEvent(organizationId, user.getId(), "EMPLOYEE_ROLE_UPDATED", "EMPLOYEE", employee.getId().toString(), oldRoles, targetRole.getCode());
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
                String email = req.getEmail() != null ? req.getEmail().toLowerCase().trim() : null;
                if (email == null || email.isBlank()) {
                    result.getErrors().add("Row " + row + ": Email is required");
                    result.setTotalFailed(result.getTotalFailed() + 1);
                    continue;
                }

                String code = req.getEmployeeCode() != null ? req.getEmployeeCode().trim() : null;
                if (!StringUtils.hasText(code)) {
                    code = employeeNumberGenerator.generateNextEmployeeCode(organizationId);
                } else if (employeeRepository.existsByOrganizationIdAndEmployeeCode(organizationId, code)) {
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
                    UserEntity targetUser = userRepository.findByIdAndOrganizationId(req.getUserId(), organizationId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", req.getUserId()));
                    if (targetUser.getClientId() != null) {
                        throw new BusinessValidationException("Cannot link a client portal user to an employee account");
                    }
                    if (employeeRepository.findByOrganizationIdAndUserId(organizationId, targetUser.getId()).isPresent()) {
                        throw new DuplicateResourceException("Employee", "userId", targetUser.getId());
                    }
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
                UserEntity user = userRepository.findByIdAndOrganizationId(targetUserId, organizationId).orElse(null);
                if (user != null) {
                    sendEmployeeInvitation(saved, user, organizationId);
                }
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

    @Override
    @Transactional
    public void resendInvitation(UUID employeeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        EmployeeEntity employee = employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        if (employee.getUserId() == null) {
            throw new BusinessValidationException("Cannot resend invitation: employee has no linked user account");
        }

        UserEntity user = userRepository.findByIdAndOrganizationId(employee.getUserId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", employee.getUserId()));

        sendEmployeeInvitation(employee, user, organizationId);
    }

    private void sendEmployeeInvitation(EmployeeEntity employee, UserEntity user, UUID organizationId) {
        try {
            // Invalidate any existing pending activation tokens for this user
            organizationActivationTokenRepository.invalidateAllPendingTokensForUser(user.getId(), Instant.now());

            // Generate Secure Activation Token (SHA-256 hashed at rest)
            String rawToken = PasswordSecurityUtils.generateSecureToken();
            String tokenHash = PasswordSecurityUtils.hashSha256(rawToken);
            Instant expiresAt = Instant.now().plus(activationExpirationHours, ChronoUnit.HOURS);

            OrganizationActivationTokenEntity activationToken = OrganizationActivationTokenEntity.builder()
                    .userId(user.getId())
                    .organizationId(organizationId)
                    .tokenHash(tokenHash)
                    .expiresAt(expiresAt)
                    .build();
            organizationActivationTokenRepository.save(activationToken);

            String orgName = organizationRepository.findById(organizationId)
                    .map(OrganizationEntity::getName)
                    .orElse("Your Practice");

            String activationUrl = activationBaseUrl + "?token=" + rawToken;

            emailNotificationService.sendEmployeeInvitationEmail(
                    employee.getEmail(),
                    employee.getFullName(),
                    orgName,
                    employee.getDesignation(),
                    activationUrl,
                    activationExpirationHours
            );

            auditService.logEvent(
                    organizationId,
                    user.getId(),
                    "EMPLOYEE_INVITATION_SENT",
                    "EMPLOYEE",
                    employee.getId().toString(),
                    null,
                    "Dispatched invitation email with activation link to " + employee.getEmail()
            );
        } catch (Exception ex) {
            log.error("Failed to send employee invitation email to {}: {}", employee.getEmail(), ex.getMessage(), ex);
            auditService.logEvent(
                    organizationId,
                    user.getId(),
                    "EMPLOYEE_INVITATION_FAILED",
                    "EMPLOYEE",
                    employee.getId().toString(),
                    null,
                    "Failed to dispatch invitation email to " + employee.getEmail() + ": " + ex.getMessage()
            );
        }
    }
}
