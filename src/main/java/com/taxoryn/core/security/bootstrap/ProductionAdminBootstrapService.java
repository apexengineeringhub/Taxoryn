package com.taxoryn.core.security.bootstrap;

import com.taxoryn.core.security.PasswordSecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationEntity.SubscriptionPlan;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.PermissionRepository;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Production Administrator Secure Bootstrap Service.
 * <p>
 * Provides a one-time, replay-safe, auditable bootstrap mechanism for provisioning
 * the initial Platform Super Administrator in production environments via environment variables.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionAdminBootstrapService implements SmartInitializingSingleton {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Value("${taxoryn.bootstrap.admin.email:${TAXORYN_BOOTSTRAP_ADMIN_EMAIL:}}")
    private String bootstrapEmail;

    @Value("${taxoryn.bootstrap.admin.password:${TAXORYN_BOOTSTRAP_ADMIN_PASSWORD:}}")
    private String bootstrapPassword;

    @Value("${taxoryn.bootstrap.admin.phone:${TAXORYN_BOOTSTRAP_ADMIN_PHONE:+918000000001}}")
    private String bootstrapPhone;

    @Value("${taxoryn.bootstrap.admin.first-name:${TAXORYN_BOOTSTRAP_ADMIN_FIRST_NAME:Taxoryn}}")
    private String firstName;

    @Value("${taxoryn.bootstrap.admin.last-name:${TAXORYN_BOOTSTRAP_ADMIN_LAST_NAME:SuperAdmin}}")
    private String lastName;

    @Override
    public void afterSingletonsInstantiated() {
        if (StringUtils.hasText(bootstrapEmail) && StringUtils.hasText(bootstrapPassword)) {
            bootstrapProductionAdmin();
        }
    }

    @Transactional
    public boolean bootstrapProductionAdmin() {
        String email = bootstrapEmail.trim().toLowerCase();
        String password = bootstrapPassword.trim();

        log.info("Evaluating production SuperAdmin bootstrap request for '{}'...", email);

        // 1. Password Strength and Known Weak Password Check
        if (!PasswordSecurityUtils.isStrongProductionPassword(password)) {
            String error = "CRITICAL SECURITY VIOLATION: Bootstrap password does not meet production complexity standards (must be >= 12 chars, include upper/lower/digits/special, and not be a known weak or demo password)";
            log.error(error);
            throw new IllegalStateException(error);
        }

        // 2. Replay Safety: Check if any active TAXORYN_SUPERADMIN already exists
        RoleEntity superAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("TAXORYN_SUPERADMIN").orElse(null);
        if (superAdminRole != null) {
            boolean hasActiveSuperAdmin = userRepository.findAll().stream()
                    .anyMatch(u -> u.getStatus() == UserStatus.ACTIVE && u.getRoles().contains(superAdminRole));
            if (hasActiveSuperAdmin) {
                log.warn("Production bootstrap REPLAY BLOCKED: An active TAXORYN_SUPERADMIN already exists in the database. Bootstrap skipped.");
                return false;
            }
        }

        // 3. Ensure Platform Governance Root Organization
        OrganizationEntity org = organizationRepository.findByEmailIgnoreCase("admin@taxoryn.com")
                .orElseGet(() -> organizationRepository.save(OrganizationEntity.builder()
                        .name("Taxoryn Platform Operations")
                        .legalName("Taxoryn Platform Technologies Pvt Ltd")
                        .email("admin@taxoryn.com")
                        .pan("AABFA0000K")
                        .status(OrganizationStatus.ACTIVE)
                        .subscriptionPlan(SubscriptionPlan.ENTERPRISE)
                        .build()));

        // 4. Ensure Roles & Permissions
        Set<RoleEntity> adminRoles = ensureSuperAdminRoles();

        // 5. Create or Activate Bootstrap User
        Optional<UserEntity> existing = userRepository.findByEmailIgnoreCase(email);
        UserEntity adminUser;
        if (existing.isPresent()) {
            adminUser = existing.get();
            adminUser.setStatus(UserStatus.ACTIVE);
            adminUser.setPasswordHash(passwordEncoder.encode(password));
            adminUser.setRoles(adminRoles);
            adminUser.setOrganizationId(org.getId());
        } else {
            adminUser = UserEntity.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .phone(bootstrapPhone)
                    .passwordHash(passwordEncoder.encode(password))
                    .status(UserStatus.ACTIVE)
                    .organizationId(org.getId())
                    .roles(adminRoles)
                    .build();
        }

        UserEntity saved = userRepository.save(adminUser);

        // 6. Record Audit Event (Never log passwords)
        try {
            auditService.logEvent(
                    "BOOTSTRAP_SUPERADMIN_CREATED",
                    "USER",
                    saved.getId().toString(),
                    null,
                    Map.of("email", saved.getEmail(), "status", saved.getStatus().name())
            );
        } catch (Exception ex) {
            log.warn("Failed recording audit log for bootstrap admin creation", ex);
        }

        log.info("Production platform SuperAdmin bootstrap completed successfully for user ID: {}", saved.getId());
        return true;
    }

    private Set<RoleEntity> ensureSuperAdminRoles() {
        List<PermissionEntity> allPermissions = permissionRepository.findAll();
        Set<PermissionEntity> platformPerms = allPermissions.stream()
                .filter(p -> p.getCode().startsWith("PLATFORM_")
                        || p.getCode().startsWith("PRACTICE_")
                        || p.getCode().startsWith("USER_")
                        || p.getCode().startsWith("MARKETPLACE_")
                        || p.getCode().startsWith("SUBSCRIPTION_")
                        || p.getCode().startsWith("PAYMENT_")
                        || p.getCode().startsWith("FEEDBACK_")
                        || p.getCode().startsWith("CONTENT_")
                        || p.getCode().startsWith("SECURITY_")
                        || p.getCode().startsWith("AUDIT_")
                        || p.getCode().startsWith("ROLE_")
                        || p.getCode().equals("ORGANIZATION_VIEW")
                )
                .collect(Collectors.toSet());

        RoleEntity taxorynSuperAdmin = roleRepository.findByCodeAndIsSystemRoleTrue("TAXORYN_SUPERADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("TAXORYN_SUPERADMIN")
                        .name("Taxoryn Platform SuperAdmin")
                        .description("Full platform administrative and operations authority")
                        .isSystemRole(true)
                        .build()));
        taxorynSuperAdmin.setPermissions(platformPerms);
        taxorynSuperAdmin = roleRepository.save(taxorynSuperAdmin);

        RoleEntity superAdminLegacy = roleRepository.findByCodeAndIsSystemRoleTrue("SUPER_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("SUPER_ADMIN")
                        .name("Platform Super Administrator")
                        .description("Platform Super Administrator with global administrative control")
                        .isSystemRole(true)
                        .build()));
        superAdminLegacy.setPermissions(platformPerms);
        superAdminLegacy = roleRepository.save(superAdminLegacy);

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(taxorynSuperAdmin);
        roles.add(superAdminLegacy);
        return roles;
    }
}
