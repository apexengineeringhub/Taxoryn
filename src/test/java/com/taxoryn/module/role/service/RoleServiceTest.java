package com.taxoryn.module.role.service;

import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.role.dto.AssignUserRolesRequest;
import com.taxoryn.module.role.dto.CreateRoleRequest;
import com.taxoryn.module.role.dto.PermissionDto;
import com.taxoryn.module.role.dto.RoleDto;
import com.taxoryn.module.role.dto.UpdateRoleRequest;
import com.taxoryn.module.role.dto.UserRolesResponse;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.mapper.RoleMapper;
import com.taxoryn.module.role.repository.PermissionRepository;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private com.taxoryn.module.audit.service.AuditService auditService;

    @InjectMocks
    private RoleServiceImpl roleService;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .organizationId(tenantId)
                .email("admin@taxpractice.com")
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("ROLE_READ", "ROLE_WRITE"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Create custom role successfully within tenant")
    void testCreateCustomRoleSuccess() {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .code("SENIOR_AUDITOR")
                .name("Senior Auditor")
                .description("Auditing lead")
                .permissionCodes(Set.of("CLIENT_VIEW", "TASK_VIEW", "GST_VIEW"))
                .build();

        when(roleRepository.findByCodeAndIsSystemRoleTrue("SENIOR_AUDITOR")).thenReturn(Optional.empty());
        when(roleRepository.existsByCodeAndOrganizationId("SENIOR_AUDITOR", tenantId)).thenReturn(false);

        PermissionEntity p1 = PermissionEntity.builder().code("CLIENT_VIEW").name("View Clients").build();
        when(permissionRepository.findByCodeIn(request.getPermissionCodes())).thenReturn(List.of(p1));

        RoleEntity saved = RoleEntity.builder()
                .code("SENIOR_AUDITOR")
                .name("Senior Auditor")
                .organizationId(tenantId)
                .isSystemRole(false)
                .permissions(new HashSet<>(Set.of(p1)))
                .build();
        saved.setId(UUID.randomUUID());

        when(roleRepository.save(any(RoleEntity.class))).thenReturn(saved);
        when(roleMapper.toDto(saved)).thenReturn(RoleDto.builder().id(saved.getId()).code("SENIOR_AUDITOR").build());

        RoleDto result = roleService.createCustomRole(request);

        assertNotNull(result);
        assertEquals("SENIOR_AUDITOR", result.getCode());
    }

    @Test
    @DisplayName("Create custom role fails on duplicate role code")
    void testCreateCustomRoleDuplicateCodeThrows() {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .code("ORG_ADMIN")
                .name("Duplicate Role")
                .permissionCodes(Set.of("CLIENT_VIEW"))
                .build();

        RoleEntity systemRole = RoleEntity.builder().code("ORG_ADMIN").isSystemRole(true).build();
        when(roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")).thenReturn(Optional.of(systemRole));

        assertThrows(DuplicateResourceException.class, () -> roleService.createCustomRole(request));
    }

    @Test
    @DisplayName("Update system default role throws ForbiddenException")
    void testUpdateSystemRoleForbiddenThrows() {
        UUID systemRoleId = UUID.randomUUID();
        RoleEntity systemRole = RoleEntity.builder().code("MANAGER").isSystemRole(true).build();
        systemRole.setId(systemRoleId);

        when(roleRepository.findById(systemRoleId)).thenReturn(Optional.of(systemRole));

        UpdateRoleRequest request = UpdateRoleRequest.builder()
                .name("Modified Manager")
                .permissionCodes(Set.of("CLIENT_VIEW"))
                .build();

        assertThrows(ForbiddenException.class, () -> roleService.updateCustomRole(systemRoleId, request));
    }

    @Test
    @DisplayName("Delete system default role throws ForbiddenException")
    void testDeleteSystemRoleForbiddenThrows() {
        UUID systemRoleId = UUID.randomUUID();
        RoleEntity systemRole = RoleEntity.builder().code("ACCOUNTANT").isSystemRole(true).build();
        systemRole.setId(systemRoleId);

        when(roleRepository.findById(systemRoleId)).thenReturn(Optional.of(systemRole));

        assertThrows(ForbiddenException.class, () -> roleService.deleteCustomRole(systemRoleId));
    }

    @Test
    @DisplayName("Assign roles to user within tenant")
    void testAssignRolesToUser() {
        UUID targetUserId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .email("staff@taxpractice.com")
                .roles(new HashSet<>())
                .build();
        user.setId(targetUserId);
        user.setOrganizationId(tenantId);

        when(userRepository.findByIdAndOrganizationId(targetUserId, tenantId)).thenReturn(Optional.of(user));

        RoleEntity role = RoleEntity.builder().code("TAX_PROFESSIONAL").isSystemRole(true).permissions(new HashSet<>()).build();
        role.setId(UUID.randomUUID());
        when(roleRepository.findByCodesAndOrganizationId(Set.of("TAX_PROFESSIONAL"), tenantId)).thenReturn(List.of(role));

        when(userRepository.save(user)).thenReturn(user);
        when(roleMapper.toDto(role)).thenReturn(RoleDto.builder().id(role.getId()).code("TAX_PROFESSIONAL").build());

        AssignUserRolesRequest request = new AssignUserRolesRequest(Set.of("TAX_PROFESSIONAL"));
        UserRolesResponse response = roleService.assignRolesToUser(targetUserId, request);

        assertNotNull(response);
        assertEquals(targetUserId, response.getUserId());
    }

    @Test
    @DisplayName("Privilege Escalation: Org Admin cannot assign platform role SUPER_ADMIN")
    void testPrivilegeEscalationAssignPlatformRoleThrows() {
        UUID targetUserId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .email("staff@taxpractice.com")
                .roles(new HashSet<>())
                .build();
        user.setId(targetUserId);
        user.setOrganizationId(tenantId);

        when(userRepository.findByIdAndOrganizationId(targetUserId, tenantId)).thenReturn(Optional.of(user));

        AssignUserRolesRequest request = new AssignUserRolesRequest(Set.of("SUPER_ADMIN"));
        assertThrows(ForbiddenException.class, () -> roleService.assignRolesToUser(targetUserId, request));
    }

    @Test
    @DisplayName("Privilege Escalation: Org Admin cannot create custom role with platform permission")
    void testPrivilegeEscalationCreateRoleWithPlatformPermissionThrows() {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .code("CUSTOM_ADMIN")
                .name("Custom Admin")
                .permissionCodes(Set.of("PLATFORM_USER_CREATE"))
                .build();

        assertThrows(ForbiddenException.class, () -> roleService.createCustomRole(request));
    }

    @Test
    @DisplayName("Privilege Escalation: Cannot create custom role with reserved platform role code")
    void testCreateCustomRoleWithReservedPlatformRoleCodeThrows() {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .code("TAXORYN_SUPERADMIN")
                .name("Fake SuperAdmin")
                .permissionCodes(Set.of("CLIENT_VIEW"))
                .build();

        assertThrows(ForbiddenException.class, () -> roleService.createCustomRole(request));
    }

    @Test
    @DisplayName("Lockout Prevention: Cannot demote sole remaining Org Admin")
    void testLockoutPreventionDemoteSoleOrgAdminThrows() {
        UUID targetUserId = UUID.randomUUID();
        RoleEntity orgAdminRole = RoleEntity.builder().code("ORG_ADMIN").isSystemRole(true).build();
        UserEntity user = UserEntity.builder()
                .email("admin@taxpractice.com")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        user.setId(targetUserId);
        user.setOrganizationId(tenantId);

        when(userRepository.findByIdAndOrganizationId(targetUserId, tenantId)).thenReturn(Optional.of(user));

        RoleEntity viewerRole = RoleEntity.builder().code("VIEWER").isSystemRole(true).permissions(new HashSet<>()).build();
        when(roleRepository.findByCodesAndOrganizationId(Set.of("VIEWER"), tenantId)).thenReturn(List.of(viewerRole));
        when(userRepository.countActiveOrgAdmins(tenantId)).thenReturn(1L);

        AssignUserRolesRequest request = new AssignUserRolesRequest(Set.of("VIEWER"));
        assertThrows(com.taxoryn.core.exception.BusinessValidationException.class,
                () -> roleService.assignRolesToUser(targetUserId, request));
    }

    @Test
    @DisplayName("Self-Escalation: Non-admin manager cannot assign themselves ORG_ADMIN")
    void testSelfPrivilegeEscalationDenied() {
        UUID managerUserId = UUID.randomUUID();
        SecurityUser managerPrincipal = SecurityUser.builder()
                .userId(managerUserId)
                .organizationId(tenantId)
                .email("manager@taxpractice.com")
                .roles(Set.of("MANAGER"))
                .permissions(Set.of("USER_UPDATE", "ROLE_WRITE"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(managerPrincipal, null, managerPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserEntity managerUser = UserEntity.builder()
                .email("manager@taxpractice.com")
                .roles(new HashSet<>())
                .build();
        managerUser.setId(managerUserId);
        managerUser.setOrganizationId(tenantId);

        when(userRepository.findByIdAndOrganizationId(managerUserId, tenantId)).thenReturn(Optional.of(managerUser));

        AssignUserRolesRequest request = new AssignUserRolesRequest(Set.of("ORG_ADMIN"));
        assertThrows(ForbiddenException.class, () -> roleService.assignRolesToUser(managerUserId, request));
    }
}
