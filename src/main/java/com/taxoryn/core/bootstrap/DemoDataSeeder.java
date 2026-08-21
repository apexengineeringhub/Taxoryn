package com.taxoryn.core.bootstrap;

import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.itr.entity.ItrProfileEntity;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrProfileStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ClientRepository clientRepository;
    private final ItrProfileRepository itrProfileRepository;
    private final ItrReturnRepository itrReturnRepository;
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

        seedDemoClientsAndItrProfiles(org);
    }

    private void seedDemoClientsAndItrProfiles(OrganizationEntity org) {
        if (clientRepository.countByOrganizationId(org.getId()) > 0) {
            return;
        }

        record DemoClient(String pan, String displayName, String legalName, ClientEntity.ClientType clientType,
                          TaxpayerType taxpayerType, ItrType defaultItrType, String email, String phone) {}

        List<DemoClient> demoClients = List.of(
                new DemoClient("ABCDE1234F", "Pawan Pathak & Associates", "Pawan Pathak & Associates", ClientEntity.ClientType.PARTNERSHIP, TaxpayerType.FIRM, ItrType.ITR_5, "pawan.tax@example.com", "9820112233"),
                new DemoClient("AABFA1234F", "MAA MUNDESHWARI ENTERPRISES", "MAA MUNDESHWARI ENTERPRISES PVT LTD", ClientEntity.ClientType.PRIVATE_LIMITED, TaxpayerType.COMPANY, ItrType.ITR_6, "mundeshwari.ent@example.com", "9833445566"),
                new DemoClient("BNZPS8821M", "Dr. Rajesh Sharma", "Dr. Rajesh Sharma", ClientEntity.ClientType.INDIVIDUAL, TaxpayerType.INDIVIDUAL, ItrType.ITR_1, "rajesh.sharma@example.com", "9811223344"),
                new DemoClient("CLXPT4412K", "Sneha Kulkarni", "Sneha Kulkarni", ClientEntity.ClientType.INDIVIDUAL, TaxpayerType.INDIVIDUAL, ItrType.ITR_2, "sneha.k@example.com", "9822334455"),
                new DemoClient("DKRPJ9931L", "Vikram Mehta (Consulting)", "Vikram Mehta", ClientEntity.ClientType.PROPRIETORSHIP, TaxpayerType.INDIVIDUAL, ItrType.ITR_3, "vikram.mehta@example.com", "9833445577"),
                new DemoClient("ELMPR3321Q", "Rohan Deshmukh (Retailer)", "Rohan Deshmukh", ClientEntity.ClientType.PROPRIETORSHIP, TaxpayerType.INDIVIDUAL, ItrType.ITR_4, "rohan.retail@example.com", "9844556677"),
                new DemoClient("FGKPA7712N", "Aarav Gupta HUF", "Aarav Gupta HUF", ClientEntity.ClientType.INDIVIDUAL, TaxpayerType.HUF, ItrType.ITR_2, "aarav.huf@example.com", "9855667788"),
                new DemoClient("AAATR5566D", "Shri Mundeshwari Seva Trust", "Shri Mundeshwari Seva Trust", ClientEntity.ClientType.TRUST, TaxpayerType.TRUST, ItrType.ITR_7, "trust.seva@example.com", "9866778899")
        );

        for (DemoClient d : demoClients) {
            ClientEntity client = ClientEntity.builder()
                    .displayName(d.displayName())
                    .legalName(d.legalName())
                    .pan(d.pan())
                    .clientType(d.clientType())
                    .email(d.email())
                    .phone(d.phone())
                    .status(ClientEntity.ClientStatus.ACTIVE)
                    .build();
            client.setOrganizationId(org.getId());
            ClientEntity savedClient = clientRepository.save(client);

            ItrProfileEntity profile = ItrProfileEntity.builder()
                    .clientId(savedClient.getId())
                    .pan(d.pan())
                    .taxpayerType(d.taxpayerType())
                    .defaultItrType(d.defaultItrType())
                    .residentialStatus(ItrProfileEntity.ResidentialStatus.RESIDENT)
                    .status(ItrProfileStatus.ACTIVE)
                    .build();
            profile.setOrganizationId(org.getId());
            itrProfileRepository.save(profile);

            LocalDate dueDate = (d.taxpayerType() == TaxpayerType.COMPANY || d.defaultItrType() == ItrType.ITR_6) ? LocalDate.of(2026, 10, 31) : LocalDate.of(2026, 7, 31);
            ItrReturnEntity returnEntity = ItrReturnEntity.builder()
                    .clientId(savedClient.getId())
                    .itrProfileId(profile.getId())
                    .assessmentYear("2026-27")
                    .financialYear("2025-26")
                    .itrType(d.defaultItrType())
                    .taxpayerType(d.taxpayerType())
                    .dueDate(dueDate)
                    .status(ItrStatus.DOCUMENTS_PENDING)
                    .build();
            returnEntity.setOrganizationId(org.getId());
            itrReturnRepository.save(returnEntity);
        }
        log.info("Seeded 8 demo clients & ITR returns for organization {}", org.getName());
    }
}
