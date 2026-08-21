package com.taxoryn.core.security;

import com.taxoryn.module.employee.entity.EmployeeEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class PracticeSecurityScope {

    public enum RoleTier {
        FIRM_ADMIN,
        DEPARTMENT_MANAGER,
        STAFF_INDIVIDUAL
    }

    private final RoleTier roleTier;
    private final UUID organizationId;
    private final UUID userId;
    private final UUID employeeId;
    private final String userEmail;
    private final String department;
    private final EmployeeEntity employee;
    private final Set<UUID> accessibleAssigneeIds;
    private final boolean isFirmAdmin;
    private final boolean isDepartmentManager;
    private final boolean isStaff;

    public boolean isFirmAdmin() {
        return isFirmAdmin;
    }

    public boolean isDepartmentManager() {
        return isDepartmentManager;
    }

    public boolean isStaff() {
        return isStaff;
    }

    public static PracticeSecurityScope firmAdmin(UUID userId) {
        return PracticeSecurityScope.builder()
                .roleTier(RoleTier.FIRM_ADMIN)
                .userId(userId)
                .isFirmAdmin(true)
                .build();
    }

    public static PracticeSecurityScope staffIndividual(UUID userId, UUID employeeId, Set<UUID> accessibleIds) {
        return PracticeSecurityScope.builder()
                .roleTier(RoleTier.STAFF_INDIVIDUAL)
                .userId(userId)
                .employeeId(employeeId)
                .accessibleAssigneeIds(accessibleIds)
                .isStaff(true)
                .build();
    }
}
