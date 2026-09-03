package com.taxoryn.core.security;

import com.taxoryn.core.exception.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RbacPrivilegeEscalationSecurityTest {

    private UUID tenantId;
    private UUID adminUserId;
    private UUID managerUserId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        managerUserId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void authenticateUser(UUID userId, Set<String> roles, Set<String> permissions) {
        SecurityUser user = SecurityUser.builder()
                .userId(userId)
                .organizationId(tenantId)
                .email("user@taxoryn.com")
                .roles(roles)
                .permissions(permissions)
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(tenantId);
    }

    @Test
    @DisplayName("Tenant Admin: Attempt to assign Platform Role (SUPER_ADMIN) must be DENIED")
    void testTenantAdminCannotAssignSuperAdmin() {
        authenticateUser(adminUserId, Set.of("ORG_ADMIN"), Set.of("USER_UPDATE", "ROLE_WRITE"));

        UUID targetUserId = UUID.randomUUID();
        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("SUPER_ADMIN"), targetUserId));
    }

    @Test
    @DisplayName("Tenant Admin: Attempt to assign Taxoryn Operations Admin must be DENIED")
    void testTenantAdminCannotAssignOperationsAdmin() {
        authenticateUser(adminUserId, Set.of("ORG_ADMIN"), Set.of("USER_UPDATE", "ROLE_WRITE"));

        UUID targetUserId = UUID.randomUUID();
        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("TAXORYN_OPERATIONS_ADMIN"), targetUserId));
    }

    @Test
    @DisplayName("Tenant Admin: Can assign standard tenant roles (MANAGER, TAX_PROFESSIONAL)")
    void testTenantAdminCanAssignTenantRoles() {
        authenticateUser(adminUserId, Set.of("ORG_ADMIN"), Set.of("USER_UPDATE", "ROLE_WRITE"));

        UUID targetUserId = UUID.randomUUID();
        assertDoesNotThrow(() ->
                SecurityUtils.validateRoleDelegation(Set.of("MANAGER", "TAX_PROFESSIONAL", "ACCOUNTANT"), targetUserId));
    }

    @Test
    @DisplayName("Non-Admin Manager: Self-Privilege Escalation to ORG_ADMIN must be DENIED")
    void testManagerCannotSelfEscalateToOrgAdmin() {
        authenticateUser(managerUserId, Set.of("MANAGER"), Set.of("USER_UPDATE", "ROLE_WRITE"));

        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("ORG_ADMIN"), managerUserId));
    }

    @Test
    @DisplayName("Non-Admin Manager: Cannot delegate roles beyond own held roles")
    void testManagerCannotDelegateUnpossessedRoles() {
        authenticateUser(managerUserId, Set.of("MANAGER"), Set.of("USER_UPDATE", "ROLE_WRITE"));

        UUID otherUserId = UUID.randomUUID();
        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("TAX_PROFESSIONAL"), otherUserId));
    }

    @Test
    @DisplayName("Tenant Admin: Cannot include Platform Permissions in custom role")
    void testTenantAdminCannotGrantPlatformPermissions() {
        authenticateUser(adminUserId, Set.of("ORG_ADMIN"), Set.of("ROLE_WRITE"));

        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validatePermissionDelegation(Set.of("PLATFORM_USER_CREATE")));
    }

    @Test
    @DisplayName("Tenant Admin: Can include standard Practice Permissions in custom role")
    void testTenantAdminCanGrantPracticePermissions() {
        authenticateUser(adminUserId, Set.of("ORG_ADMIN"), Set.of("ROLE_WRITE"));

        assertDoesNotThrow(() ->
                SecurityUtils.validatePermissionDelegation(Set.of("CLIENT_VIEW", "GST_CREATE", "ITR_VIEW")));
    }

    @Test
    @DisplayName("Platform SuperAdmin: Allowed to assign all roles and permissions")
    void testPlatformSuperAdminAllowedAll() {
        UUID superAdminId = UUID.randomUUID();
        authenticateUser(superAdminId, Set.of("TAXORYN_SUPERADMIN"), Set.of("SUPER_ADMIN", "PLATFORM_USER_CREATE"));

        UUID targetUserId = UUID.randomUUID();
        assertDoesNotThrow(() ->
                SecurityUtils.validateRoleDelegation(Set.of("SUPER_ADMIN", "TAXORYN_OPERATIONS_ADMIN"), targetUserId));
        assertDoesNotThrow(() ->
                SecurityUtils.validatePermissionDelegation(Set.of("PLATFORM_USER_CREATE", "SYSTEM_STATUS_VIEW")));
    }
}
