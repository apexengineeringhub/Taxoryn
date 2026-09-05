package com.taxoryn.module.user.controller;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.dto.CreatePlatformUserRequest;
import com.taxoryn.module.user.dto.UpdatePlatformUserRoleRequest;
import com.taxoryn.module.user.dto.UserDto;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.mapper.UserMapper;
import com.taxoryn.module.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping({"/api/v1/admin/users", "/api/admin/users"})
@RequiredArgsConstructor
@Tag(name = "Platform Admin User Governance", description = "Endpoints for platform SuperAdmin and Operations Admins to monitor, govern, provision and manage internal Taxoryn users")
@SecurityRequirement(name = "BearerAuth")
public class AdminUserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    private static final Set<String> ALLOWED_TAXORYN_PLATFORM_ROLES = Set.of(
            "TAXORYN_SUPERADMIN",
            "TAXORYN_OPERATIONS_ADMIN",
            "TAXORYN_SUPPORT_ADMIN",
            "TAXORYN_MARKETPLACE_ADMIN",
            "TAXORYN_FINANCE_ADMIN",
            "TAXORYN_CONTENT_ADMIN",
            "TAXORYN_SECURITY_ADMIN",
            "TAXORYN_ENGINEERING_ADMIN"
    );

    @GetMapping
    @PreAuthorize("hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasRole('TAXORYN_SUPPORT_ADMIN') or hasRole('TAXORYN_SECURITY_ADMIN') or hasAuthority('PLATFORM_USER_VIEW')")
    @Transactional(readOnly = true)
    @Operation(summary = "List platform users", description = "Retrieves paginated list of users across the platform with filtering by role category, status, and search query.")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getPlatformUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserEntity> pageResult = userRepository.findAll(pageable);

        List<UserEntity> filtered = pageResult.getContent().stream()
                .filter(u -> {
                    if (StringUtils.hasText(role) && !"ALL".equalsIgnoreCase(role)) {
                        boolean hasMatchingRole = u.getRoles() != null && u.getRoles().stream().anyMatch(r -> {
                            String code = r.getCode();
                            if (role.equalsIgnoreCase(code)) return true;
                            if ("SUPERADMIN".equalsIgnoreCase(role) && ("SUPER_ADMIN".equalsIgnoreCase(code) || "TAXORYN_SUPERADMIN".equalsIgnoreCase(code))) return true;
                            if ("OPERATIONS".equalsIgnoreCase(role) && "TAXORYN_OPERATIONS_ADMIN".equalsIgnoreCase(code)) return true;
                            if ("SUPPORT".equalsIgnoreCase(role) && "TAXORYN_SUPPORT_ADMIN".equalsIgnoreCase(code)) return true;
                            if ("MARKETPLACE".equalsIgnoreCase(role) && "TAXORYN_MARKETPLACE_ADMIN".equalsIgnoreCase(code)) return true;
                            if ("FINANCE".equalsIgnoreCase(role) && "TAXORYN_FINANCE_ADMIN".equalsIgnoreCase(code)) return true;
                            if ("CONTENT".equalsIgnoreCase(role) && "TAXORYN_CONTENT_ADMIN".equalsIgnoreCase(code)) return true;
                            if ("SECURITY".equalsIgnoreCase(role) && "TAXORYN_SECURITY_ADMIN".equalsIgnoreCase(code)) return true;
                            if ("ENGINEERING".equalsIgnoreCase(role) && "TAXORYN_ENGINEERING_ADMIN".equalsIgnoreCase(code)) return true;
                            if ("PRACTITIONERS".equalsIgnoreCase(role) && ("ORG_ADMIN".equalsIgnoreCase(code) || "PRACTICE_ADMIN".equalsIgnoreCase(code) || "PRACTICE_OWNER".equalsIgnoreCase(code) || "PRACTITIONER".equalsIgnoreCase(code))) return true;
                            if ("STAFF".equalsIgnoreCase(role) && ("STAFF".equalsIgnoreCase(code) || "ARTICLE_ASSISTANT".equalsIgnoreCase(code) || "PRACTICE_EMPLOYEE".equalsIgnoreCase(code))) return true;
                            if ("CUSTOMERS".equalsIgnoreCase(role) && ("CLIENT_USER".equalsIgnoreCase(code) || "CLIENT_ADMIN".equalsIgnoreCase(code) || "MARKETPLACE_CUSTOMER".equalsIgnoreCase(code))) return true;
                            return false;
                        });
                        if (!hasMatchingRole) return false;
                    }
                    if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
                        if (u.getStatus() == null || !u.getStatus().name().equalsIgnoreCase(status)) {
                            return false;
                        }
                    }
                    if (StringUtils.hasText(search)) {
                        String q = search.toLowerCase().trim();
                        boolean matchesEmail = u.getEmail() != null && u.getEmail().toLowerCase().contains(q);
                        boolean matchesName = drillDownMatchesName(u, q);
                        boolean matchesPhone = u.getPhone() != null && u.getPhone().contains(q);
                        if (!matchesEmail && !matchesName && !matchesPhone) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        List<UserDto> dtos = userMapper.toDtoList(filtered);
        PagedResponse<UserDto> response = PagedResponse.<UserDto>builder()
                .content(dtos)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isFirst(pageResult.isFirst())
                .isLast(pageResult.isLast())
                .hasNext(pageResult.hasNext())
                .hasPrevious(pageResult.hasPrevious())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Platform users retrieved successfully", response));
    }

    private boolean drillDownMatchesName(UserEntity u, String q) {
        return (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(q))
                || (u.getLastName() != null && u.getLastName().toLowerCase().contains(q));
    }

    @PostMapping
    @PreAuthorize("hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasAuthority('PLATFORM_USER_CREATE')")
    @Transactional
    @Operation(summary = "Create internal Taxoryn platform user", description = "Provisions a new internal platform user with controlled system role and privilege escalation prevention.")
    public ResponseEntity<ApiResponse<UserDto>> createPlatformUser(
            @Valid @RequestBody CreatePlatformUserRequest request
    ) {
        String cleanRoleCode = request.getRoleCode().trim().toUpperCase();

        // 1. Verify Role is a controlled Taxoryn platform role
        if (!ALLOWED_TAXORYN_PLATFORM_ROLES.contains(cleanRoleCode)) {
            throw new BadRequestException("Role '" + request.getRoleCode() + "' is not an approved Taxoryn platform role");
        }

        // 2. Privilege Escalation Prevention Check
        if (!SecurityUtils.canAssignPlatformRole(cleanRoleCode)) {
            throw new ForbiddenException("Privilege escalation denied: You are not authorized to assign the '" + cleanRoleCode + "' platform role");
        }

        // 3. Unique email check
        if (userRepository.existsByEmailIgnoreCase(request.getEmail().trim())) {
            throw new BadRequestException("User with email '" + request.getEmail().trim() + "' already exists");
        }

        // 4. Resolve Target Role
        RoleEntity role = roleRepository.findByCodeAndIsSystemRoleTrue(cleanRoleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "code", cleanRoleCode));

        // 5. Resolve Platform Root Org
        OrganizationEntity platformOrg = organizationRepository.findByEmailIgnoreCase("admin@taxoryn.com")
                .or(() -> organizationRepository.findAll().stream().filter(o -> "Taxoryn Platform Operations".equalsIgnoreCase(o.getName()) || "Taxoryn Platform Global".equalsIgnoreCase(o.getName())).findFirst())
                .orElseGet(() -> organizationRepository.findAll().stream().findFirst().orElse(null));

        String rawPassword = StringUtils.hasText(request.getTemporaryPassword())
                ? request.getTemporaryPassword()
                : com.taxoryn.core.security.PasswordSecurityUtils.generateSecureTemporaryPassword();

        UserEntity user = UserEntity.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .status(request.getStatus() != null ? request.getStatus() : UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(role)))
                .build();

        if (platformOrg != null) {
            user.setOrganizationId(platformOrg.getId());
        }

        UserEntity saved = userRepository.save(user);

        // 6. Record Authoritative Audit Event
        auditService.logEvent(
                "TAXORYN_USER_CREATED",
                "USER",
                saved.getId().toString(),
                null,
                Map.of("email", saved.getEmail(), "role", cleanRoleCode, "status", saved.getStatus().name())
        );

        log.info("Created Taxoryn Platform User: email={}, role={}, caller={}",
                saved.getEmail(), cleanRoleCode, SecurityUtils.getCurrentUserEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Taxoryn platform user created successfully", userMapper.toDto(saved)));
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasAuthority('PLATFORM_USER_UPDATE')")
    @Transactional
    @Operation(summary = "Update platform user role", description = "Reassigns a platform user to a new approved platform role with privilege escalation protection.")
    public ResponseEntity<ApiResponse<UserDto>> updatePlatformUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdatePlatformUserRoleRequest request
    ) {
        String cleanRoleCode = request.getRoleCode().trim().toUpperCase();

        if (!ALLOWED_TAXORYN_PLATFORM_ROLES.contains(cleanRoleCode)) {
            throw new BadRequestException("Role '" + request.getRoleCode() + "' is not an approved Taxoryn platform role");
        }

        if (!SecurityUtils.canAssignPlatformRole(cleanRoleCode)) {
            throw new ForbiddenException("Privilege escalation denied: You are not authorized to assign the '" + cleanRoleCode + "' platform role");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Block self-role modification for non-superadmin
        UUID currentUserId = SecurityUtils.getCurrentUser().map(com.taxoryn.core.security.SecurityUser::getUserId).orElse(null);
        if (currentUserId != null && currentUserId.equals(userId) && !SecurityUtils.isTaxorynSuperAdmin()) {
            throw new ForbiddenException("Self-role mutation denied: You cannot modify your own platform role");
        }

        // Prevent non-superadmin from modifying an existing SuperAdmin
        boolean targetIsSuperAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "SUPER_ADMIN".equals(r.getCode()) || "TAXORYN_SUPERADMIN".equals(r.getCode()));

        if (targetIsSuperAdmin && !SecurityUtils.isTaxorynSuperAdmin()) {
            throw new ForbiddenException("Only Taxoryn SuperAdmin can modify another SuperAdmin account");
        }

        RoleEntity newRole = roleRepository.findByCodeAndIsSystemRoleTrue(cleanRoleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "code", cleanRoleCode));

        String previousRoles = user.getRoles() != null
                ? user.getRoles().stream().map(RoleEntity::getCode).collect(Collectors.joining(", "))
                : "NONE";

        user.setRoles(new HashSet<>(Set.of(newRole)));
        UserEntity saved = userRepository.save(user);

        // Audit Record
        auditService.logEvent(
                "TAXORYN_USER_ROLE_CHANGED",
                "USER",
                saved.getId().toString(),
                Map.of("previousRoles", previousRoles),
                Map.of("newRole", cleanRoleCode)
        );

        log.info("Updated Taxoryn user role: id={}, email={}, newRole={}, caller={}",
                saved.getId(), saved.getEmail(), cleanRoleCode, SecurityUtils.getCurrentUserEmail());

        return ResponseEntity.ok(ApiResponse.success("User role updated to " + cleanRoleCode, userMapper.toDto(saved)));
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasAuthority('PLATFORM_USER_UPDATE')")
    @Transactional
    @Operation(summary = "Update user status", description = "Allows platform admins to activate, suspend, or deactivate a platform user.")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam UserEntity.UserStatus status
    ) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Prevent non-superadmin from modifying an existing SuperAdmin status
        boolean targetIsSuperAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "SUPER_ADMIN".equals(r.getCode()) || "TAXORYN_SUPERADMIN".equals(r.getCode()));

        if (targetIsSuperAdmin && !SecurityUtils.isTaxorynSuperAdmin()) {
            throw new ForbiddenException("Only Taxoryn SuperAdmin can modify SuperAdmin account status");
        }

        String oldStatus = user.getStatus() != null ? user.getStatus().name() : "UNKNOWN";
        user.setStatus(status);
        UserEntity saved = userRepository.save(user);

        // Audit Record
        String actionCode = (status == UserEntity.UserStatus.SUSPENDED || status == UserEntity.UserStatus.INACTIVE)
                ? "TAXORYN_USER_DISABLED"
                : "TAXORYN_USER_STATUS_UPDATED";

        auditService.logEvent(
                actionCode,
                "USER",
                saved.getId().toString(),
                Map.of("previousStatus", oldStatus),
                Map.of("newStatus", status.name())
        );

        log.info("Updated Taxoryn user status: id={}, email={}, status={}, caller={}",
                saved.getId(), saved.getEmail(), status, SecurityUtils.getCurrentUserEmail());

        return ResponseEntity.ok(ApiResponse.success("User status updated to " + status, userMapper.toDto(saved)));
    }
}
