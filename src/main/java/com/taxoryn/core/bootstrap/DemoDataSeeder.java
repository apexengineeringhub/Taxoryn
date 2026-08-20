package com.taxoryn.core.bootstrap;

import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationEntity.SubscriptionPlan;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            seedRoles();
            seedDemoOrganizationAndAdmin("contact@apextax.com", "Apex Tax Advisors LLP", "AABFA1234K", "admin@apextax.com", "Admin", "User");
            seedDemoOrganizationAndAdmin("pawanadv@gmail.com", "MAA MUNDESHWARI TAX CONSULTANCY", "AABFA1234F", "pawanadv@gmail.com", "Pawan", "Pathak");
        } catch (Exception ex) {
            log.warn("DemoDataSeeder warning (non-fatal): {}", ex.getMessage());
        }
    }

    private void seedRoles() {
        if (roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").isEmpty()) {
            roleRepository.save(RoleEntity.builder()
                    .code("ORG_ADMIN")
                    .name("Organization Administrator")
                    .isSystemRole(true)
                    .build());
        }
        if (roleRepository.findByCodeAndIsSystemRoleTrue("PRACTITIONER").isEmpty()) {
            roleRepository.save(RoleEntity.builder()
                    .code("PRACTITIONER")
                    .name("Tax Practitioner / CA")
                    .isSystemRole(true)
                    .build());
        }
        if (roleRepository.findByCodeAndIsSystemRoleTrue("ARTICLE_ASSISTANT").isEmpty()) {
            roleRepository.save(RoleEntity.builder()
                    .code("ARTICLE_ASSISTANT")
                    .name("Article Assistant")
                    .isSystemRole(true)
                    .build());
        }
    }

    private void seedDemoOrganizationAndAdmin(String orgEmail, String orgName, String pan, String adminEmail, String firstName, String lastName) {
        OrganizationEntity org = organizationRepository.findByEmailIgnoreCase(orgEmail)
                .orElseGet(() -> organizationRepository.save(OrganizationEntity.builder()
                        .name(orgName)
                        .email(orgEmail)
                        .pan(pan)
                        .status(OrganizationStatus.ACTIVE)
                        .subscriptionPlan(SubscriptionPlan.ENTERPRISE)
                        .build()));

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .build()));

        Optional<UserEntity> existingUser = userRepository.findByEmailIgnoreCase(adminEmail);
        if (existingUser.isEmpty()) {
            UserEntity adminUser = UserEntity.builder()
                    .email(adminEmail.toLowerCase().trim())
                    .passwordHash(passwordEncoder.encode("Password123!"))
                    .firstName(firstName)
                    .lastName(lastName)
                    .status(UserStatus.ACTIVE)
                    .roles(new HashSet<>(Set.of(orgAdminRole)))
                    .build();
            adminUser.setOrganizationId(org.getId());
            userRepository.save(adminUser);
            log.info("Demo user seeded: {} ({}) with password Password123!", adminEmail, orgName);
        } else {
            // Ensure active status and proper password hash
            UserEntity user = existingUser.get();
            if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches("Password123!", user.getPasswordHash())) {
                user.setStatus(UserStatus.ACTIVE);
                user.setPasswordHash(passwordEncoder.encode("Password123!"));
                userRepository.save(user);
                log.info("Demo user updated/reactivated: {}", adminEmail);
            }
        }
    }
}
