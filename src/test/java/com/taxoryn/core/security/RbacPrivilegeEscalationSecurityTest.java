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
    private UUID staffUserId;
    private UUID practitionerUserId;
    private UUID clientUserId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        managerUserId = UUID.randomUUID();
        staffUserId = UUID.randomUUID();
        practitionerUserId = UUID.randomUUID();
        clientUserId = UUID.randomUUID();
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

    // =========================================================================
    // 1. Role Assignment & Delegation
    // =========================================================================

    @Test
    @DisplayName("[PASS] Authorized tenant role assignment by ORG_ADMIN")
    void testTenantAdminCanAssignTenantRoles() {
        authenticateUser(adminUserId, Set.of("ORG_ADMIN"), Set.of("USER_UPDATE", "ROLE_WRITE"));

        UUID targetUserId = UUID.randomUUID();
        assertDoesNotThrow(() ->
                SecurityUtils.validateRoleDelegation(Set.of("MANAGER", "TAX_PROFESSIONAL", "ACCOUNTANT", "EMPLOYEE"), targetUserId));
    }

    @Test
    @DisplayName("[PASS] Platform role assignment denied for Tenant Admin")
    void testTenantAdminCannotAssignPlatformRoles() {
        authenticateUser(adminUserId, Set.of("ORG_ADMIN"), Set.of("USER_UPDATE", "ROLE_WRITE"));

        UUID targetUserId = UUID.randomUUID();
        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("SUPER_ADMIN"), targetUserId));
        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("TAXORYN_SUPERADMIN"), targetUserId));
        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("TAXORYN_OPERATIONS_ADMIN"), targetUserId));
        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("TAXORYN_SECURITY_ADMIN"), targetUserId));
    }

    @Test
    @DisplayName("[PASS] Unauthorized role assignment denied (STAFF assigning ORG_ADMIN)")
    void testStaffCannotAssignOrgAdmin() {
        authenticateUser(staffUserId, Set.of("STAFF"), Set.of("USER_VIEW"));

        UUID otherUserId = UUID.randomUUID();
        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("ORG_ADMIN"), otherUserId));
    }

    // =========================================================================
    // 2. Self-Escalation Scenarios
    // =========================================================================

    @Test
    @DisplayName("[PASS] CLIENT_USER cannot self-escalate to CLIENT_ADMIN")
    void testClientUserCannotSelfEscalateToClientAdmin() {
        authenticateUser(clientUserId, Set.of("CLIENT_USER"), Set.of("PORTAL_ACCESS"));

        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("CLIENT_ADMIN"), clientUserId));
    }

    @Test
    @DisplayName("[PASS] CLIENT_USER cannot self-escalate to STAFF")
    void testClientUserCannotSelfEscalateToStaff() {
        authenticateUser(clientUserId, Set.of("CLIENT_USER"), Set.of("PORTAL_ACCESS"));

        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("STAFF"), clientUserId));
    }

    @Test
    @DisplayName("[PASS] STAFF cannot self-escalate to PRACTITIONER")
    void testStaffCannotSelfEscalateToPractitioner() {
        authenticateUser(staffUserId, Set.of("STAFF"), Set.of("TASK_VIEW"));

        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("PRACTITIONER"), staffUserId));
    }

    @Test
    @DisplayName("[PASS] STAFF cannot self-escalate to ORG_ADMIN")
    void testStaffCannotSelfEscalateToOrgAdmin() {
        authenticateUser(staffUserId, Set.of("STAFF"), Set.of("TASK_VIEW"));

        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("ORG_ADMIN"), staffUserId));
    }

    @Test
    @DisplayName("[PASS] PRACTITIONER cannot self-escalate to ORG_ADMIN")
    void testPractitionerCannotSelfEscalateToOrgAdmin() {
        authenticateUser(practitionerUserId, Set.of("PRACTITIONER"), Set.of("CLIENT_VIEW"));

        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("ORG_ADMIN"), practitionerUserId));
    }

    @Test
    @DisplayName("[PASS] ORG_ADMIN cannot self-escalate to platform admin")
    void testOrgAdminCannotSelfEscalateToPlatformAdmin() {
        authenticateUser(adminUserId, Set.of("ORG_ADMIN"), Set.of("USER_UPDATE", "ROLE_WRITE"));

        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("TAXORYN_SUPERADMIN"), adminUserId));
        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("SUPER_ADMIN"), adminUserId));
    }

    // =========================================================================
    // 3. Other-User Promotion
    // =========================================================================

    @Test
    @DisplayName("[PASS] Unauthorized user cannot promote another user")
    void testManagerCannotPromoteAnotherUserToAdmin() {
        authenticateUser(managerUserId, Set.of("MANAGER"), Set.of("USER_UPDATE"));

        UUID otherUserId = UUID.randomUUID();
        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validateRoleDelegation(Set.of("ORG_ADMIN"), otherUserId));
    }

    // =========================================================================
    // 4. Custom Role Permission Delegation
    // =========================================================================

    @Test
    @DisplayName("[PASS] Tenant Admin: Platform permission cannot be granted in custom role")
    void testTenantAdminCannotGrantPlatformPermissions() {
        authenticateUser(adminUserId, Set.of("ORG_ADMIN"), Set.of("ROLE_WRITE"));

        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validatePermissionDelegation(Set.of("PLATFORM_USER_CREATE")));
        assertThrows(ForbiddenException.class, () ->
                SecurityUtils.validatePermissionDelegation(Set.of("SYSTEM_STATUS_VIEW")));
    }

    @Test
    @DisplayName("[PASS] Tenant Admin: Can include standard Practice Permissions in custom role")
    void testTenantAdminCanGrantPracticePermissions() {
        authenticateUser(adminUserId, Set.of("ORG_ADMIN"), Set.of("ROLE_WRITE"));

        assertDoesNotThrow(() ->
                SecurityUtils.validatePermissionDelegation(Set.of("CLIENT_VIEW", "GST_CREATE", "ITR_VIEW")));
    }

    @Test
    @DisplayName("[PASS] Authorized tenant role assignment by PRACTICE_OWNER and PRACTICE_ADMIN")
    void testPracticeOwnerAndAdminCanAssignTenantRoles() {
        authenticateUser(adminUserId, Set.of("PRACTICE_OWNER"), Set.of("USER_UPDATE", "ROLE_WRITE"));
        UUID targetUserId = UUID.randomUUID();
        assertDoesNotThrow(() ->
                SecurityUtils.validateRoleDelegation(Set.of("TAX_PROFESSIONAL", "STAFF"), targetUserId));

        authenticateUser(adminUserId, Set.of("PRACTICE_ADMIN"), Set.of("USER_UPDATE", "ROLE_WRITE"));
        assertDoesNotThrow(() ->
                SecurityUtils.validateRoleDelegation(Set.of("ACCOUNTANT", "EMPLOYEE"), targetUserId));
    }

    @Test
    @DisplayName("[PASS] Role category classifications verify accurately")
    void testRoleCategoryClassifications() {
        org.junit.jupiter.api.Assertions.assertTrue(SecurityUtils.isPlatformRole("SUPER_ADMIN"));
        org.junit.jupiter.api.Assertions.assertTrue(SecurityUtils.isPlatformRole("TAXORYN_SUPERADMIN"));
        org.junit.jupiter.api.Assertions.assertTrue(SecurityUtils.isPlatformRole("TAXORYN_SECURITY_ADMIN"));
        org.junit.jupiter.api.Assertions.assertFalse(SecurityUtils.isPlatformRole("ORG_ADMIN"));

        org.junit.jupiter.api.Assertions.assertTrue(SecurityUtils.isTenantRole("ORG_ADMIN"));
        org.junit.jupiter.api.Assertions.assertTrue(SecurityUtils.isTenantRole("PRACTICE_OWNER"));
        org.junit.jupiter.api.Assertions.assertTrue(SecurityUtils.isTenantRole("TAX_PROFESSIONAL"));
        org.junit.jupiter.api.Assertions.assertFalse(SecurityUtils.isTenantRole("SUPER_ADMIN"));

        org.junit.jupiter.api.Assertions.assertTrue(SecurityUtils.isClientRole("CLIENT_ADMIN"));
        org.junit.jupiter.api.Assertions.assertTrue(SecurityUtils.isClientRole("CLIENT_USER"));
        org.junit.jupiter.api.Assertions.assertTrue(SecurityUtils.isClientRole("MARKETPLACE_CUSTOMER"));

        org.junit.jupiter.api.Assertions.assertTrue(SecurityUtils.isPrivilegedRole("ORG_ADMIN"));
        org.junit.jupiter.api.Assertions.assertTrue(SecurityUtils.isPrivilegedRole("MANAGER"));
        org.junit.jupiter.api.Assertions.assertTrue(SecurityUtils.isPrivilegedRole("SUPER_ADMIN"));
    }

    @Test
    @DisplayName("[PASS] Platform SuperAdmin: Allowed to assign all roles and permissions")
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
