package com.taxoryn.core.bootstrap;

import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
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
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.PermissionRepository;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final ItrProfileRepository itrProfileRepository;
    private final ItrReturnRepository itrReturnRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            seedPermissionsAndRoles();
            seedDemoOrganizationAndStaff("contact@apextax.com", "Apex Tax Advisors LLP", "AABFA1234K", "admin@apextax.com", "Admin", "User");
            seedDemoOrganizationAndStaff("pawanadv@gmail.com", "MAA MUNDESHWARI TAX CONSULTANCY", "AABFA1234F", "pawanadv@gmail.com", "Pawan", "Pathak");
        } catch (Exception ex) {
            log.warn("DemoDataSeeder warning (non-fatal): {}", ex.getMessage());
        }
    }

    private void seedPermissionsAndRoles() {
        // 1. Seed standard permissions
        List<String> permissionCodes = List.of(
                "TASK_VIEW", "TASK_READ", "TASK_CREATE", "TASK_WRITE", "TASK_UPDATE", "TASK_DELETE", "TASK_ASSIGN",
                "CLIENT_VIEW", "CLIENT_READ", "CLIENT_CREATE", "CLIENT_WRITE", "CLIENT_UPDATE", "CLIENT_DELETE",
                "GST_VIEW", "GST_READ", "GST_CREATE", "GST_WRITE", "GST_UPDATE", "GST_DELETE",
                "ITR_VIEW", "ITR_READ", "ITR_CREATE", "ITR_WRITE", "ITR_UPDATE", "ITR_DELETE",
                "DOCUMENT_VIEW", "DOCUMENT_READ", "DOCUMENT_UPLOAD", "DOCUMENT_CREATE", "DOCUMENT_UPDATE",
                "DASHBOARD_VIEW", "BILLING_VIEW", "BILLING_READ", "BILLING_CREATE", "BILLING_UPDATE",
                "EMPLOYEE_VIEW", "EMPLOYEE_READ", "EMPLOYEE_CREATE", "EMPLOYEE_WRITE", "EMPLOYEE_UPDATE",
                "ORGANIZATION_VIEW", "ROLE_READ", "USER_VIEW", "USER_WRITE"
        );

        for (String code : permissionCodes) {
            if (permissionRepository.findByCode(code).isEmpty()) {
                permissionRepository.save(PermissionEntity.builder()
                        .code(code)
                        .name(code.replace("_", " "))
                        .module(code.split("_")[0])
                        .description("Permission to " + code.toLowerCase().replace("_", " "))
                        .build());
            }
        }

        List<PermissionEntity> allPermissions = permissionRepository.findAll();
        Set<PermissionEntity> allPermSet = new HashSet<>(allPermissions);

        // 2. ORG_ADMIN Role (All Permissions)
        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .build()));
        orgAdminRole.setPermissions(allPermSet);
        roleRepository.save(orgAdminRole);

        // 3. PRACTITIONER Role (CA / Tax Practitioner)
        RoleEntity practitionerRole = roleRepository.findByCodeAndIsSystemRoleTrue("PRACTITIONER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("PRACTITIONER")
                        .name("Tax Practitioner / CA")
                        .isSystemRole(true)
                        .build()));
        practitionerRole.setPermissions(allPermSet);
        roleRepository.save(practitionerRole);

        // 4. ARTICLE_ASSISTANT Role (Staff / Trainee) - PoLP Principle of Least Privilege
        RoleEntity articleRole = roleRepository.findByCodeAndIsSystemRoleTrue("ARTICLE_ASSISTANT")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ARTICLE_ASSISTANT")
                        .name("Article Assistant")
                        .isSystemRole(true)
                        .build()));
        Set<PermissionEntity> articlePerms = allPermissions.stream()
                .filter(p -> p.getCode().startsWith("TASK_") && !p.getCode().equals("TASK_DELETE")
                        || p.getCode().startsWith("CLIENT_") && (p.getCode().contains("VIEW") || p.getCode().contains("READ"))
                        || p.getCode().startsWith("GST_") && !p.getCode().equals("GST_DELETE")
                        || p.getCode().startsWith("ITR_") && !p.getCode().equals("ITR_DELETE")
                        || p.getCode().startsWith("DOCUMENT_") && !p.getCode().equals("DOCUMENT_DELETE")
                        || p.getCode().equals("DASHBOARD_VIEW")
                        || p.getCode().equals("EMPLOYEE_VIEW") || p.getCode().equals("EMPLOYEE_READ")
                )
                .collect(java.util.stream.Collectors.toSet());
        articleRole.setPermissions(articlePerms);
        roleRepository.save(articleRole);

        // 5. STAFF Role
        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("STAFF")
                        .name("Practice Staff")
                        .isSystemRole(true)
                        .build()));
        staffRole.setPermissions(articlePerms);
        roleRepository.save(staffRole);
    }

    private void seedDemoOrganizationAndStaff(String orgEmail, String orgName, String pan, String adminEmail, String firstName, String lastName) {
        OrganizationEntity org = organizationRepository.findByEmailIgnoreCase(orgEmail)
                .orElseGet(() -> organizationRepository.save(OrganizationEntity.builder()
                        .name(orgName)
                        .email(orgEmail)
                        .pan(pan)
                        .status(OrganizationStatus.ACTIVE)
                        .subscriptionPlan(SubscriptionPlan.ENTERPRISE)
                        .build()));

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseThrow();
        RoleEntity practitionerRole = roleRepository.findByCodeAndIsSystemRoleTrue("PRACTITIONER").orElse(orgAdminRole);
        RoleEntity articleRole = roleRepository.findByCodeAndIsSystemRoleTrue("ARTICLE_ASSISTANT").orElse(practitionerRole);

        // 1. Seed or Reactivate Org Admin User
        Optional<UserEntity> existingUser = userRepository.findByEmailIgnoreCase(adminEmail);
        UserEntity adminUser;
        if (existingUser.isEmpty()) {
            adminUser = UserEntity.builder()
                    .email(adminEmail.toLowerCase().trim())
                    .passwordHash(passwordEncoder.encode("Password123!"))
                    .firstName(firstName)
                    .lastName(lastName)
                    .status(UserStatus.ACTIVE)
                    .roles(new HashSet<>(Set.of(orgAdminRole)))
                    .build();
            adminUser.setOrganizationId(org.getId());
            adminUser = userRepository.save(adminUser);
            log.info("Demo user seeded: {} ({}) with password Password123!", adminEmail, orgName);
        } else {
            adminUser = existingUser.get();
            adminUser.setStatus(UserStatus.ACTIVE);
            adminUser.setPasswordHash(passwordEncoder.encode("Password123!"));
            adminUser.getRoles().add(orgAdminRole);
            adminUser = userRepository.save(adminUser);
        }

        // 2. Seed Clients & ITR Returns first
        seedDemoClientsAndItrProfiles(org);

        // 3. Seed Practice Employees & Linked User Accounts
        seedDemoEmployeesAndTasks(org, articleRole, practitionerRole);
    }

    private void seedDemoEmployeesAndTasks(OrganizationEntity org, RoleEntity articleRole, RoleEntity practitionerRole) {
        boolean isMundeshwari = org.getName().toUpperCase().contains("MUNDESHWARI");

        record DemoEmployee(String code, String email, String firstName, String lastName, String phone,
                            String department, String designation, RoleEntity role) {}

        List<DemoEmployee> demoStaff = isMundeshwari ? List.of(
                new DemoEmployee("EMP-108", "pooja.joshi@maamundeshwari.com", "Pooja", "Joshi", "+919878901234", "Direct Tax", "Article Assistant / Trainee", articleRole),
                new DemoEmployee("EMP-102", "rajesh.patel@maamundeshwari.com", "Rajesh", "Patel", "+919811223344", "GST & Indirect Tax", "Senior Tax Accountant", practitionerRole),
                new DemoEmployee("EMP-105", "vikas.sharma@maamundeshwari.com", "Vikas", "Sharma", "+919833445566", "Audit & Assurance", "Audit Manager", practitionerRole)
        ) : List.of(
                new DemoEmployee("EMP-201", "neha.sharma@apextax.com", "Neha", "Sharma", "+919844556677", "Direct Tax", "Senior Tax Consultant", practitionerRole),
                new DemoEmployee("EMP-202", "amit.verma@apextax.com", "Amit", "Verma", "+919855667788", "Compliance", "Article Assistant", articleRole)
        );

        List<ClientEntity> clients = clientRepository.findAllByOrganizationId(org.getId());
        ClientEntity cSneha = clients.stream().filter(c -> "Sneha Kulkarni".equalsIgnoreCase(c.getDisplayName())).findFirst().orElse(null);
        ClientEntity cRajesh = clients.stream().filter(c -> "Dr. Rajesh Sharma".equalsIgnoreCase(c.getDisplayName())).findFirst().orElse(null);
        ClientEntity cVikram = clients.stream().filter(c -> c.getDisplayName().contains("Vikram Mehta")).findFirst().orElse(null);
        ClientEntity cAarav = clients.stream().filter(c -> c.getDisplayName().contains("Aarav Gupta")).findFirst().orElse(null);
        ClientEntity cPawan = clients.stream().filter(c -> c.getDisplayName().contains("Pawan Pathak")).findFirst().orElse(null);
        ClientEntity cRohan = clients.stream().filter(c -> c.getDisplayName().contains("Rohan Deshmukh")).findFirst().orElse(null);
        ClientEntity cMundeshwari = clients.stream().filter(c -> c.getDisplayName().contains("MAA MUNDESHWARI ENTERPRISES")).findFirst().orElse(null);
        ClientEntity cTrust = clients.stream().filter(c -> c.getDisplayName().contains("Shri Mundeshwari Seva Trust")).findFirst().orElse(null);

        for (DemoEmployee d : demoStaff) {
            // Seed or update UserEntity for employee login
            UserEntity user = userRepository.findByEmailIgnoreCase(d.email())
                    .orElseGet(() -> {
                        UserEntity u = UserEntity.builder()
                                .email(d.email().toLowerCase().trim())
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .firstName(d.firstName())
                                .lastName(d.lastName())
                                .phone(d.phone())
                                .status(UserStatus.ACTIVE)
                                .roles(new HashSet<>(Set.of(d.role())))
                                .build();
                        u.setOrganizationId(org.getId());
                        return userRepository.save(u);
                    });

            // Ensure active status and password match
            user.setStatus(UserStatus.ACTIVE);
            user.setPasswordHash(passwordEncoder.encode("Password123!"));
            user.getRoles().add(d.role());
            userRepository.save(user);

            // Seed or update EmployeeEntity
            EmployeeEntity employee = employeeRepository.findByOrganizationIdAndEmail(org.getId(), d.email())
                    .or(() -> employeeRepository.findByOrganizationIdAndEmployeeCode(org.getId(), d.code()))
                    .orElseGet(() -> {
                        EmployeeEntity emp = EmployeeEntity.builder()
                                .userId(user.getId())
                                .employeeCode(d.code())
                                .firstName(d.firstName())
                                .lastName(d.lastName())
                                .email(d.email())
                                .phone(d.phone())
                                .department(d.department())
                                .designation(d.designation())
                                .joiningDate(LocalDate.now().minusMonths(6))
                                .status(EmployeeStatus.ACTIVE)
                                .build();
                        emp.setOrganizationId(org.getId());
                        return emp;
                    });

            employee.setEmail(d.email());
            employee.setEmployeeCode(d.code());
            employee.setFirstName(d.firstName());
            employee.setLastName(d.lastName());
            employee.setPhone(d.phone());
            employee.setDepartment(d.department());
            employee.setDesignation(d.designation());
            employee.setStatus(EmployeeStatus.ACTIVE);
            employee.setUserId(user.getId());
            EmployeeEntity savedEmp = employeeRepository.save(employee);
            log.info("Seeded employee login: {} ({}) with password Password123!", d.email(), d.designation());

            // Assign clients according to department
            if (d.email().contains("pooja.joshi")) {
                List<ClientEntity> directTaxClients = List.of(cSneha, cRajesh, cVikram, cAarav);
                for (ClientEntity cl : directTaxClients) {
                    if (cl != null) {
                        cl.setAssignedEmployeeId(savedEmp.getId());
                        clientRepository.save(cl);
                    }
                }
            } else if (d.email().contains("rajesh.patel")) {
                List<ClientEntity> gstClients = List.of(cPawan, cRohan);
                for (ClientEntity cl : gstClients) {
                    if (cl != null) {
                        cl.setAssignedEmployeeId(savedEmp.getId());
                        clientRepository.save(cl);
                    }
                }
            } else if (d.email().contains("vikas.sharma")) {
                List<ClientEntity> auditClients = List.of(cMundeshwari, cTrust);
                for (ClientEntity cl : auditClients) {
                    if (cl != null) {
                        cl.setAssignedEmployeeId(savedEmp.getId());
                        clientRepository.save(cl);
                    }
                }
            } else if (d.email().contains("neha.sharma")) {
                List<ClientEntity> directTaxClients = List.of(cSneha, cRajesh, cVikram, cAarav);
                for (ClientEntity cl : directTaxClients) {
                    if (cl != null) {
                        cl.setAssignedEmployeeId(savedEmp.getId());
                        clientRepository.save(cl);
                    }
                }
            } else if (d.email().contains("amit.verma")) {
                List<ClientEntity> complianceClients = List.of(cPawan, cRohan);
                for (ClientEntity cl : complianceClients) {
                    if (cl != null) {
                        cl.setAssignedEmployeeId(savedEmp.getId());
                        clientRepository.save(cl);
                    }
                }
            }

            // Seed Tasks specifically assigned to this employee (if none exist)
            if (taskRepository.findAllByOrganizationIdAndAssignedTo(org.getId(), user.getId(), PageRequest.of(0, 1)).isEmpty()) {
                if (d.email().contains("pooja.joshi") || d.email().contains("neha.sharma")) {
                    seedTask(org.getId(), cSneha != null ? cSneha.getId() : null, user.getId(),
                            "ITR-2 Computation & Capital Gains Review for Sneha Kulkarni",
                            "Verify equity capital gains statements from Zerodha/Groww and compute tax under Section 112A/111A.",
                            TaskCategory.ITR, TaskPriority.HIGH, TaskStatus.IN_PROGRESS, LocalDate.now().plusDays(3));

                    seedTask(org.getId(), cRajesh != null ? cRajesh.getId() : null, user.getId(),
                            "Verify Form 26AS & AIS/TIS for Dr. Rajesh Sharma",
                            "Cross-check high-value transactions and TDS credits (194J & 194C) with Form 26AS portal data.",
                            TaskCategory.ITR, TaskPriority.MEDIUM, TaskStatus.TODO, LocalDate.now().plusDays(5));

                    seedTask(org.getId(), cAarav != null ? cAarav.getId() : null, user.getId(),
                            "HUF Capital Account Reconciliation & ITR-2 Filing for Aarav Gupta HUF",
                            "Reconcile capital accounts, gift tax exemptions, and rental TDS deductions under Section 194-IB.",
                            TaskCategory.ITR, TaskPriority.HIGH, TaskStatus.IN_PROGRESS, LocalDate.now().plusDays(2));

                    seedTask(org.getId(), cVikram != null ? cVikram.getId() : null, user.getId(),
                            "Prepare Advance Tax Q2 Working Sheet for Vikram Mehta",
                            "Compute estimated net taxable profit and calculate September 15th advance tax installment installment.",
                            TaskCategory.COMPLIANCE, TaskPriority.MEDIUM, TaskStatus.TODO, LocalDate.now().plusDays(7));
                } else if (d.email().contains("rajesh.patel") || d.email().contains("amit.verma")) {
                    seedTask(org.getId(), cPawan != null ? cPawan.getId() : null, user.getId(),
                            "Monthly GSTR-1 & 3B Return Filing for Pawan Pathak & Associates",
                            "Generate JSON payload, upload sales B2B invoices and file GSTR-3B before the 20th deadline.",
                            TaskCategory.GST, TaskPriority.URGENT, TaskStatus.TODO, LocalDate.now().plusDays(2));

                    seedTask(org.getId(), cRohan != null ? cRohan.getId() : null, user.getId(),
                            "Quarterly TDS 26Q Challan Verification & Form 16 Generation",
                            "Validate FVU return file on TRACES utility and download consolidated Form 16 certificates.",
                            TaskCategory.COMPLIANCE, TaskPriority.HIGH, TaskStatus.TODO, LocalDate.now().plusDays(6));
                } else if (d.email().contains("vikas.sharma")) {
                    seedTask(org.getId(), cMundeshwari != null ? cMundeshwari.getId() : null, user.getId(),
                            "Form 3CD Tax Audit Scrutiny for MAA MUNDESHWARI ENTERPRISES PVT LTD",
                            "Scrutinize Clause 13 (Method of Accounting), Clause 34 (TDS Compliance) and quantitative details.",
                            TaskCategory.AUDIT, TaskPriority.URGENT, TaskStatus.UNDER_REVIEW, LocalDate.now().plusDays(4));
                }
            }
        }
    }

    private void seedTask(UUID orgId, UUID clientId, UUID assignedTo, String title, String description,
                          TaskCategory category, TaskPriority priority, TaskStatus status, LocalDate dueDate) {
        TaskEntity task = TaskEntity.builder()
                .clientId(clientId)
                .assignedTo(assignedTo)
                .title(title)
                .description(description)
                .taskCategory(category)
                .priority(priority)
                .status(status)
                .dueDate(dueDate)
                .build();
        task.setOrganizationId(orgId);
        taskRepository.save(task);
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
