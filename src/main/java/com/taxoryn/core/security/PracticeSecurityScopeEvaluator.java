package com.taxoryn.core.security;

import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PracticeSecurityScopeEvaluator {

    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;
    private final TaskRepository taskRepository;

    public PracticeSecurityScope evaluateCurrentScope() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID userId = SecurityUtils.getCurrentUserId();
        String email = SecurityUtils.getCurrentUserEmail();

        if (organizationId == null || userId == null) {
            return PracticeSecurityScope.builder()
                    .roleTier(PracticeSecurityScope.RoleTier.STAFF_INDIVIDUAL)
                    .organizationId(organizationId)
                    .userId(userId)
                    .userEmail(email)
                    .isFirmAdmin(false)
                    .isDepartmentManager(false)
                    .isStaff(true)
                    .accessibleAssigneeIds(userId != null ? Set.of(userId) : Collections.emptySet())
                    .build();
        }

        // 1. Check Roles
        Set<String> roles = SecurityUtils.getCurrentRoles();
        boolean hasAdminRole = roles != null && (
                roles.contains("ORG_ADMIN") || roles.contains("ROLE_ORG_ADMIN")
                || roles.contains("SUPER_ADMIN") || roles.contains("ROLE_SUPER_ADMIN")
                || roles.contains("PARTNER") || roles.contains("ROLE_PARTNER")
        );

        // 2. Retrieve linked EmployeeEntity
        Optional<EmployeeEntity> employeeOpt = employeeRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .or(() -> email != null ? employeeRepository.findByOrganizationIdAndEmail(organizationId, email) : Optional.empty());

        EmployeeEntity employee = employeeOpt.orElse(null);
        UUID employeeId = employee != null ? employee.getId() : null;
        String department = employee != null ? employee.getDepartment() : null;

        if (hasAdminRole) {
            return PracticeSecurityScope.builder()
                    .roleTier(PracticeSecurityScope.RoleTier.FIRM_ADMIN)
                    .organizationId(organizationId)
                    .userId(userId)
                    .employeeId(employeeId)
                    .userEmail(email)
                    .department(department)
                    .employee(employee)
                    .isFirmAdmin(true)
                    .isDepartmentManager(false)
                    .isStaff(false)
                    .accessibleAssigneeIds(null) // unrestricted
                    .build();
        }

        boolean hasManagerRole = roles != null && (
                roles.contains("PRACTITIONER") || roles.contains("ROLE_PRACTITIONER")
                || roles.contains("MANAGER") || roles.contains("ROLE_MANAGER")
        );

        // Check if employee has direct reportees
        boolean hasReportees = employeeId != null && !employeeRepository.findAllByOrganizationId(organizationId)
                .stream().filter(e -> employeeId.equals(e.getManagerId())).toList().isEmpty();

        if (hasManagerRole || hasReportees) {
            // Department Manager Scope
            Set<UUID> accessibleIds = new HashSet<>();
            accessibleIds.add(userId);
            if (employeeId != null) accessibleIds.add(employeeId);

            // Include colleagues in same department
            if (department != null && !department.isBlank()) {
                List<EmployeeEntity> deptEmployees = employeeRepository.findAllByOrganizationId(organizationId).stream()
                        .filter(e -> department.equalsIgnoreCase(e.getDepartment()))
                        .toList();
                for (EmployeeEntity emp : deptEmployees) {
                    accessibleIds.add(emp.getId());
                    if (emp.getUserId() != null) accessibleIds.add(emp.getUserId());
                }
            }

            return PracticeSecurityScope.builder()
                    .roleTier(PracticeSecurityScope.RoleTier.DEPARTMENT_MANAGER)
                    .organizationId(organizationId)
                    .userId(userId)
                    .employeeId(employeeId)
                    .userEmail(email)
                    .department(department)
                    .employee(employee)
                    .isFirmAdmin(false)
                    .isDepartmentManager(true)
                    .isStaff(false)
                    .accessibleAssigneeIds(accessibleIds)
                    .build();
        }

        // Staff / Article Assistant Scope (strictly restricted to self)
        Set<UUID> selfIds = new HashSet<>();
        selfIds.add(userId);
        if (employeeId != null) selfIds.add(employeeId);

        return PracticeSecurityScope.builder()
                .roleTier(PracticeSecurityScope.RoleTier.STAFF_INDIVIDUAL)
                .organizationId(organizationId)
                .userId(userId)
                .employeeId(employeeId)
                .userEmail(email)
                .department(department)
                .employee(employee)
                .isFirmAdmin(false)
                .isDepartmentManager(false)
                .isStaff(true)
                .accessibleAssigneeIds(selfIds)
                .build();
    }

    public Set<UUID> getAccessibleClientIds(PracticeSecurityScope scope) {
        if (scope == null || scope.isFirmAdmin()) {
            return null; // unrestricted
        }

        UUID orgId = scope.getOrganizationId();
        Set<UUID> assigneeIds = scope.getAccessibleAssigneeIds();
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<UUID> accessibleClientIds = new HashSet<>();

        // 1. Clients directly assigned to these employees
        List<UUID> assignedClientIds = clientRepository.findIdsByOrganizationIdAndAssignedEmployeeIdIn(orgId, assigneeIds);
        if (assignedClientIds != null) {
            accessibleClientIds.addAll(assignedClientIds);
        }

        // 2. Clients where these employees have active tasks assigned
        List<UUID> taskClientIds = taskRepository.findClientIdsByAssignedToIn(orgId, assigneeIds);
        if (taskClientIds != null) {
            accessibleClientIds.addAll(taskClientIds);
        }

        return accessibleClientIds;
    }

    public boolean hasBillingAccess(PracticeSecurityScope scope) {
        if (scope == null) {
            return false;
        }
        if (scope.isFirmAdmin()) {
            return true;
        }
        Set<String> roles = SecurityUtils.getCurrentRoles();
        if (roles.contains("ORG_ADMIN") || roles.contains("ROLE_ORG_ADMIN")
                || roles.contains("SUPER_ADMIN") || roles.contains("ROLE_SUPER_ADMIN")
                || roles.contains("PARTNER") || roles.contains("ROLE_PARTNER")) {
            return true;
        }
        return SecurityUtils.getCurrentUser()
                .map(user -> user.getPermissions() != null && (
                        user.getPermissions().contains("BILLING_VIEW")
                        || user.getPermissions().contains("BILLING_READ")
                        || user.getPermissions().contains("BILLING_CREATE")
                ))
                .orElse(false);
    }
}
