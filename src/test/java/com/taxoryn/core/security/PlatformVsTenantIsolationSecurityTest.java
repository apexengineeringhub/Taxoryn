package com.taxoryn.core.security;

import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.module.role.dto.PermissionDto;
import com.taxoryn.module.role.dto.RoleDto;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.mapper.RoleMapper;
import com.taxoryn.module.role.repository.PermissionRepository;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.role.service.RoleServiceImpl;
import com.taxoryn.module.user.controller.AdminUserController;
import com.taxoryn.module.user.dto.CreatePlatformUserRequest;
import com.taxoryn.module.user.dto.UpdatePlatformUserRoleRequest;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformVsTenantIsolationSecurityTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    private UUID tenantId;
    private UUID tenantAdminUserId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        tenantAdminUserId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void authenticateTenantAdmin() {
        SecurityUser principal = SecurityUser.builder()
                .userId(tenantAdminUserId)
                .organizationId(tenantId)
                .email("admin@practice.com")
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("USER_VIEW", "USER_CREATE", "USER_UPDATE", "ROLE_READ", "ROLE_WRITE"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(tenantId);
    }

    private void authenticateSuperAdmin() {
        UUID superAdminId = UUID.randomUUID();
        SecurityUser principal = SecurityUser.builder()
                .userId(superAdminId)
                .organizationId(null)
                .email("superadmin@taxoryn.com")
                .roles(Set.of("TAXORYN_SUPERADMIN", "SUPER_ADMIN"))
                .permissions(Set.of("PLATFORM_USER_VIEW", "PLATFORM_USER_CREATE", "PLATFORM_USER_UPDATE"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Tenant Admin: Platform roles are filtered out from getAvailableRoles()")
    void testTenantAdminCannotViewPlatformRolesInCatalog() {
        authenticateTenantAdmin();

        RoleEntity superAdminRole = RoleEntity.builder().code("SUPER_ADMIN").name("Platform SuperAdmin").isSystemRole(true).build();
        RoleEntity orgAdminRole = RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build();
        RoleEntity managerRole = RoleEntity.builder().code("MANAGER").name("Manager").isSystemRole(true).build();

        when(roleRepository.findAllAvailableForOrganization(tenantId)).thenReturn(List.of(superAdminRole, orgAdminRole, managerRole));
        when(roleMapper.toDtoList(List.of(orgAdminRole, managerRole)))
                .thenReturn(List.of(
                        RoleDto.builder().code("ORG_ADMIN").build(),
                        RoleDto.builder().code("MANAGER").build()
                ));

        List<RoleDto> availableRoles = roleService.getAvailableRoles();

        assertEquals(2, availableRoles.size());
        assertTrue(availableRoles.stream().noneMatch(r -> "SUPER_ADMIN".equals(r.getCode())));
    }

    @Test
    @DisplayName("Tenant Admin: Platform permissions are filtered out from getAllPermissions()")
    void testTenantAdminCannotViewPlatformPermissions() {
        authenticateTenantAdmin();

        PermissionEntity p1 = PermissionEntity.builder().code("CLIENT_VIEW").name("View Clients").build();
        PermissionEntity p2 = PermissionEntity.builder().code("PLATFORM_USER_CREATE").name("Create Platform User").build();

        when(permissionRepository.findAll()).thenReturn(List.of(p1, p2));
        when(roleMapper.toPermissionDtoList(List.of(p1)))
                .thenReturn(List.of(PermissionDto.builder().code("CLIENT_VIEW").build()));

        List<PermissionDto> permissions = roleService.getAllPermissions();

        assertEquals(1, permissions.size());
        assertEquals("CLIENT_VIEW", permissions.get(0).getCode());
    }

    @Test
    @DisplayName("Tenant Admin: Direct lookup of platform role by ID is FORBIDDEN")
    void testTenantAdminDirectLookupOfPlatformRoleForbidden() {
        authenticateTenantAdmin();

        UUID platformRoleId = UUID.randomUUID();
        RoleEntity superAdminRole = RoleEntity.builder().code("SUPER_ADMIN").isSystemRole(true).build();
        superAdminRole.setId(platformRoleId);

        when(roleRepository.findById(platformRoleId)).thenReturn(Optional.of(superAdminRole));

        assertThrows(ForbiddenException.class, () -> roleService.getRoleById(platformRoleId));
    }

    @Test
    @DisplayName("Platform SuperAdmin: Receives all roles including platform roles in catalog")
    void testSuperAdminReceivesAllRoles() {
        authenticateSuperAdmin();

        RoleEntity superAdminRole = RoleEntity.builder().code("SUPER_ADMIN").name("Platform SuperAdmin").isSystemRole(true).build();
        RoleEntity orgAdminRole = RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build();

        when(roleRepository.findAllAvailableForOrganization(null)).thenReturn(List.of(superAdminRole, orgAdminRole));
        when(roleMapper.toDtoList(List.of(superAdminRole, orgAdminRole)))
                .thenReturn(List.of(
                        RoleDto.builder().code("SUPER_ADMIN").build(),
                        RoleDto.builder().code("ORG_ADMIN").build()
                ));

        List<RoleDto> availableRoles = roleService.getAvailableRoles();

        assertEquals(2, availableRoles.size());
    }
}
