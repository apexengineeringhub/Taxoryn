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
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.entity.InvoiceItemEntity;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.gst.entity.GstProfileEntity;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstProfileStatus;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstType;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstReturnType;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.portal.entity.ClientDocumentRequestEntity;
import com.taxoryn.module.portal.entity.ClientDocumentRequestEntity.RequestStatus;
import com.taxoryn.module.portal.repository.ClientDocumentRequestRepository;
import com.taxoryn.module.tds.entity.TdsChallanEntity;
import com.taxoryn.module.tds.entity.TdsProfileEntity;
import com.taxoryn.module.tds.entity.TdsReturnEntity;
import com.taxoryn.module.tds.entity.TdsReturnEntity.FvuValidationStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFormType;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import com.taxoryn.module.tds.repository.TdsChallanRepository;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceServiceEntity;
import com.taxoryn.module.marketplace.repository.MarketplaceLeadRepository;
import com.taxoryn.module.marketplace.repository.MarketplaceProfileRepository;
import com.taxoryn.module.marketplace.repository.MarketplaceServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile({"dev", "demo"})
@org.springframework.core.annotation.Order(1)
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
    private final GstProfileRepository gstProfileRepository;
    private final GstReturnFilingRepository gstReturnFilingRepository;
    private final TdsProfileRepository tdsProfileRepository;
    private final TdsReturnRepository tdsReturnRepository;
    private final TdsChallanRepository tdsChallanRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClientDocumentRequestRepository docRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final MarketplaceProfileRepository marketplaceProfileRepository;
    private final MarketplaceServiceRepository marketplaceServiceRepository;
    private final MarketplaceLeadRepository marketplaceLeadRepository;

    @Override
    public void run(String... args) {
        try {
            seedPermissionsAndRoles();
        } catch (Exception ex) {
            log.error("Failed seeding permissions and roles", ex);
        }
        try {
            seedDemoOrganizationAndStaff("contact@apextax.com", "Apex Tax Advisors LLP", "AABFA1234K", "admin@apextax.com", "Admin", "User");
        } catch (Exception ex) {
            log.error("Failed seeding Apex Tax Advisors LLP", ex);
        }
        try {
            seedDemoOrganizationAndStaff("pawanadv@gmail.com", "MAA MUNDESHWARI TAX CONSULTANCY", "AABFA1234F", "pawanadv@gmail.com", "Pawan", "Pathak");
        } catch (Exception ex) {
            log.error("Failed seeding MAA MUNDESHWARI TAX CONSULTANCY", ex);
        }
    }

    private void seedPermissionsAndRoles() {
        // 1. Seed standard permissions
        List<String> permissionCodes = List.of(
                "TASK_VIEW", "TASK_READ", "TASK_CREATE", "TASK_WRITE", "TASK_UPDATE", "TASK_DELETE", "TASK_ASSIGN",
                "CLIENT_VIEW", "CLIENT_READ", "CLIENT_CREATE", "CLIENT_WRITE", "CLIENT_UPDATE", "CLIENT_DELETE",
                "GST_VIEW", "GST_READ", "GST_CREATE", "GST_WRITE", "GST_UPDATE", "GST_DELETE",
                "ITR_VIEW", "ITR_READ", "ITR_CREATE", "ITR_WRITE", "ITR_UPDATE", "ITR_DELETE",
                "TDS_VIEW", "TDS_READ", "TDS_CREATE", "TDS_WRITE", "TDS_UPDATE", "TDS_DELETE",
                "DOCUMENT_VIEW", "DOCUMENT_READ", "DOCUMENT_UPLOAD", "DOCUMENT_CREATE", "DOCUMENT_UPDATE",
                "DASHBOARD_VIEW", "BILLING_VIEW", "BILLING_READ", "BILLING_CREATE", "BILLING_UPDATE",
                "EMPLOYEE_VIEW", "EMPLOYEE_READ", "EMPLOYEE_CREATE", "EMPLOYEE_WRITE", "EMPLOYEE_UPDATE",
                "ORGANIZATION_VIEW", "ROLE_READ", "USER_VIEW", "USER_WRITE",
                "CLIENT_PORTAL_ACCESS", "CLIENT_PORTAL_PROFILE_VIEW", "CLIENT_PORTAL_STATUS_VIEW",
                "CLIENT_PORTAL_DOCUMENT_VIEW", "CLIENT_PORTAL_DOCUMENT_UPLOAD", "CLIENT_PORTAL_INVOICE_VIEW",
                "CLIENT_PORTAL_USER_MANAGE"
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
                        || p.getCode().startsWith("TDS_") && !p.getCode().equals("TDS_DELETE")
                        || p.getCode().startsWith("DOCUMENT_") && !p.getCode().equals("DOCUMENT_DELETE")
                        || p.getCode().equals("DASHBOARD_VIEW")
                        || p.getCode().equals("EMPLOYEE_VIEW") || p.getCode().equals("EMPLOYEE_READ")
                )
                .collect(Collectors.toSet());
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

        // 6. CLIENT_USER Role (Portal Access, Views & Uploads)
        RoleEntity clientUserRole = roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_USER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("CLIENT_USER")
                        .name("Client User")
                        .description("Client staff with access to view compliance status and upload documents")
                        .isSystemRole(true)
                        .build()));
        Set<PermissionEntity> clientUserPerms = allPermissions.stream()
                .filter(p -> p.getCode().startsWith("CLIENT_PORTAL_") && !p.getCode().equals("CLIENT_PORTAL_USER_MANAGE"))
                .collect(Collectors.toSet());
        clientUserRole.setPermissions(clientUserPerms);
        roleRepository.save(clientUserRole);

        // 7. CLIENT_ADMIN Role (Full Portal Admin)
        RoleEntity clientAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("CLIENT_ADMIN")
                        .name("Client Administrator")
                        .description("Client admin with full portal access and user management")
                        .isSystemRole(true)
                        .build()));
        Set<PermissionEntity> clientAdminPerms = allPermissions.stream()
                .filter(p -> p.getCode().startsWith("CLIENT_PORTAL_"))
                .collect(Collectors.toSet());
        clientAdminRole.setPermissions(clientAdminPerms);
        roleRepository.save(clientAdminRole);
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
            if (adminUser.getRoles() == null) {
                adminUser.setRoles(new HashSet<>());
            }
            adminUser.getRoles().add(orgAdminRole);
            adminUser = userRepository.save(adminUser);
        }

        // 2. Seed Clients & ITR Returns first
        seedDemoClientsAndItrProfiles(org);

        // 3. Seed Practice Employees & Linked User Accounts
        seedDemoEmployeesAndTasks(org, articleRole, practitionerRole);

        // 4. Seed Demo Client Customer Logins & Portal Data
        seedDemoClientUsersAndPortalData(org);

        // 5. Seed Demo Marketplace Profile, Services & Inbound Leads
        seedDemoMarketplaceProfile(org);
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
            if (user.getRoles() == null) {
                user.setRoles(new HashSet<>());
            }
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
                List<ClientEntity> directTaxClients = java.util.stream.Stream.of(cSneha, cRajesh, cVikram, cAarav).filter(java.util.Objects::nonNull).toList();
                for (ClientEntity cl : directTaxClients) {
                    cl.setAssignedEmployeeId(savedEmp.getId());
                    clientRepository.save(cl);
                }
            } else if (d.email().contains("rajesh.patel")) {
                List<ClientEntity> gstClients = java.util.stream.Stream.of(cPawan, cRohan).filter(java.util.Objects::nonNull).toList();
                for (ClientEntity cl : gstClients) {
                    cl.setAssignedEmployeeId(savedEmp.getId());
                    clientRepository.save(cl);
                }
            } else if (d.email().contains("vikas.sharma")) {
                List<ClientEntity> auditClients = java.util.stream.Stream.of(cMundeshwari, cTrust).filter(java.util.Objects::nonNull).toList();
                for (ClientEntity cl : auditClients) {
                    cl.setAssignedEmployeeId(savedEmp.getId());
                    clientRepository.save(cl);
                }
            } else if (d.email().contains("neha.sharma")) {
                List<ClientEntity> directTaxClients = java.util.stream.Stream.of(cSneha, cRajesh, cVikram, cAarav).filter(java.util.Objects::nonNull).toList();
                for (ClientEntity cl : directTaxClients) {
                    cl.setAssignedEmployeeId(savedEmp.getId());
                    clientRepository.save(cl);
                }
            } else if (d.email().contains("amit.verma")) {
                List<ClientEntity> complianceClients = java.util.stream.Stream.of(cPawan, cRohan).filter(java.util.Objects::nonNull).toList();
                for (ClientEntity cl : complianceClients) {
                    cl.setAssignedEmployeeId(savedEmp.getId());
                    clientRepository.save(cl);
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

        List<ClientEntity> existingClients = clientRepository.findAllByOrganizationId(org.getId());

        for (DemoClient d : demoClients) {
            ClientEntity savedClient = existingClients.stream()
                    .filter(c -> d.pan().equalsIgnoreCase(c.getPan()) || d.displayName().equalsIgnoreCase(c.getDisplayName()))
                    .findFirst()
                    .orElseGet(() -> {
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
                        return clientRepository.save(client);
                    });

            if (itrProfileRepository.findByOrganizationIdAndClientId(org.getId(), savedClient.getId()).isEmpty()) {
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
        }
        log.info("Verified demo clients & ITR returns for organization {}", org.getName());
    }

    private void seedDemoClientUsersAndPortalData(OrganizationEntity org) {
        RoleEntity clientUserRole = roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_USER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("CLIENT_USER")
                        .name("Client User")
                        .description("Client staff with access to view compliance status and upload documents")
                        .isSystemRole(true)
                        .build()));

        RoleEntity clientAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("CLIENT_ADMIN")
                        .name("Client Administrator")
                        .description("Client admin with full portal access and user management")
                        .isSystemRole(true)
                        .build()));

        List<ClientEntity> clients = clientRepository.findAllByOrganizationId(org.getId());
        if (clients.isEmpty()) return;

        boolean isMundeshwari = org.getName().toUpperCase().contains("MUNDESHWARI");

        record DemoCustomer(String email, String firstName, String lastName, String clientSearchName, RoleEntity role) {}

        List<DemoCustomer> demoCustomers = isMundeshwari ? List.of(
                new DemoCustomer("client.mundeshwari@maamundeshwari.com", "Mundeshwari", "Director", "MAA MUNDESHWARI", clientAdminRole),
                new DemoCustomer("client.pawanassoc@maamundeshwari.com", "Pawan", "Pathak", "Pawan Pathak & Associates", clientUserRole),
                new DemoCustomer("client.sneha@maamundeshwari.com", "Sneha", "Kulkarni", "Sneha Kulkarni", clientUserRole),
                new DemoCustomer("client.rajesh@maamundeshwari.com", "Dr. Rajesh", "Sharma", "Dr. Rajesh Sharma", clientUserRole)
        ) : List.of(
                new DemoCustomer("client.sneha@apextax.com", "Sneha", "Kulkarni", "Sneha Kulkarni", clientUserRole),
                new DemoCustomer("client.rajesh@apextax.com", "Dr. Rajesh", "Sharma", "Dr. Rajesh Sharma", clientAdminRole),
                new DemoCustomer("client.vikram@apextax.com", "Vikram", "Mehta", "Vikram Mehta", clientUserRole)
        );

        for (DemoCustomer dc : demoCustomers) {
            ClientEntity client = clients.stream()
                    .filter(c -> c.getDisplayName().toLowerCase().contains(dc.clientSearchName().toLowerCase()))
                    .findFirst()
                    .orElse(clients.get(0));

            UserEntity clientUser = userRepository.findByEmailIgnoreCase(dc.email())
                    .orElseGet(() -> {
                        UserEntity u = UserEntity.builder()
                                .email(dc.email().toLowerCase().trim())
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .firstName(dc.firstName())
                                .lastName(dc.lastName())
                                .phone("+919800112233")
                                .status(UserStatus.ACTIVE)
                                .clientId(client.getId())
                                .roles(new HashSet<>(Set.of(dc.role())))
                                .build();
                        u.setOrganizationId(org.getId());
                        return userRepository.save(u);
                    });

            clientUser.setClientId(client.getId());
            clientUser.setStatus(UserStatus.ACTIVE);
            clientUser.setPasswordHash(passwordEncoder.encode("Password123!"));
            if (clientUser.getRoles() == null) {
                clientUser.setRoles(new HashSet<>());
            }
            clientUser.getRoles().add(dc.role());
            userRepository.save(clientUser);
            log.info("Seeded demo client customer login: {} ({}) for client {}", dc.email(), dc.role().getCode(), client.getDisplayName());

            // 1. Seed demo Invoice if none exists
            if (invoiceRepository.findAllByOrganizationIdAndClientIdOrderByInvoiceDateDesc(org.getId(), client.getId()).isEmpty()) {
                InvoiceEntity invoice = InvoiceEntity.builder()
                        .clientId(client.getId())
                        .invoiceNumber("INV-2026-" + String.format("%04d", (int) (Math.random() * 9000) + 1000))
                        .invoiceDate(LocalDate.now().minusDays(15))
                        .dueDate(LocalDate.now().plusDays(15))
                        .subtotal(new BigDecimal("15000.00"))
                        .tax(new BigDecimal("2700.00"))
                        .total(new BigDecimal("17700.00"))
                        .paidAmount(new BigDecimal("7700.00"))
                        .balanceDue(new BigDecimal("10000.00"))
                        .status(InvoiceStatus.PARTIALLY_PAID)
                        .notes("Annual Statutory Compliance & Direct Tax Advisory Fee")
                        .terms("Net 30 Days. 18% GST Applicable.")
                        .build();
                invoice.setOrganizationId(org.getId());

                InvoiceItemEntity item1 = InvoiceItemEntity.builder()
                        .service(InvoiceItemEntity.BillingServiceType.ITR_FILING)
                        .description("ITR Computation, E-filing & Capital Gains Reconciliation")
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("10000.00"))
                        .taxRate(new BigDecimal("18.00"))
                        .tax(new BigDecimal("1800.00"))
                        .amount(new BigDecimal("11800.00"))
                        .build();
                InvoiceItemEntity item2 = InvoiceItemEntity.builder()
                        .service(InvoiceItemEntity.BillingServiceType.CONSULTING)
                        .description("Advance Tax Consultation & Form 26AS Audit")
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("5000.00"))
                        .taxRate(new BigDecimal("18.00"))
                        .tax(new BigDecimal("900.00"))
                        .amount(new BigDecimal("5900.00"))
                        .build();
                invoice.addItem(item1);
                invoice.addItem(item2);
                invoiceRepository.save(invoice);
            }

            // 2. Seed demo Pending Document Request if none exists
            if (docRequestRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(org.getId(), client.getId()).isEmpty()) {
                ClientDocumentRequestEntity docReq = ClientDocumentRequestEntity.builder()
                        .clientId(client.getId())
                        .title("FY 2025-26 Bank Statement (Savings & Current)")
                        .description("Please upload complete 12-month PDF statement with interest certificates for accurate capital gains computation.")
                        .documentType(DocumentType.BANK_STATEMENT)
                        .dueDate(LocalDate.now().plusDays(5))
                        .financialYear("2025-26")
                        .assessmentYear("2026-27")
                        .status(RequestStatus.PENDING)
                        .build();
                docReq.setOrganizationId(org.getId());
                docRequestRepository.save(docReq);
            }

            // 3. Seed demo GST Profile & Filing if client has GSTIN
            if (gstProfileRepository.findByOrganizationIdAndClientId(org.getId(), client.getId()).isEmpty()) {
                GstProfileEntity gstProfile = GstProfileEntity.builder()
                        .clientId(client.getId())
                        .gstin(client.getPan() != null ? "27" + client.getPan() + "1Z5" : "27AABFA1234K1Z5")
                        .legalName(client.getLegalName())
                        .tradeName(client.getDisplayName())
                        .gstType(GstType.REGULAR)
                        .status(GstProfileStatus.ACTIVE)
                        .build();
                gstProfile.setOrganizationId(org.getId());
                GstProfileEntity savedGstProfile = gstProfileRepository.save(gstProfile);

                GstReturnFilingEntity gstr1 = GstReturnFilingEntity.builder()
                        .gstProfileId(savedGstProfile.getId())
                        .clientId(client.getId())
                        .returnType(GstReturnType.GSTR1)
                        .returnPeriod("May 2026")
                        .financialYear("2026-27")
                        .dueDate(LocalDate.of(2026, 6, 11))
                        .filingStatus(GstFilingStatus.FILED)
                        .filingDate(LocalDate.of(2026, 6, 10))
                        .acknowledgementNumber("AA2705260192837")
                        .totalTaxableValue(new BigDecimal("250000.00"))
                        .totalTaxLiability(new BigDecimal("45000.00"))
                        .build();
                gstr1.setOrganizationId(org.getId());
                gstReturnFilingRepository.save(gstr1);

                GstReturnFilingEntity gstr3b = GstReturnFilingEntity.builder()
                        .gstProfileId(savedGstProfile.getId())
                        .clientId(client.getId())
                        .returnType(GstReturnType.GSTR3B)
                        .returnPeriod("May 2026")
                        .financialYear("2026-27")
                        .dueDate(LocalDate.of(2026, 6, 20))
                        .filingStatus(GstFilingStatus.FILED)
                        .filingDate(LocalDate.of(2026, 6, 18))
                        .acknowledgementNumber("AA2705260849201")
                        .totalTaxableValue(new BigDecimal("250000.00"))
                        .totalTaxLiability(new BigDecimal("45000.00"))
                        .totalItcClaimed(new BigDecimal("22000.00"))
                        .taxPaidCash(new BigDecimal("23000.00"))
                        .build();
                gstr3b.setOrganizationId(org.getId());
                gstReturnFilingRepository.save(gstr3b);
            }

            // 4. Seed demo TDS Profile & Filing
            if (tdsProfileRepository.findByOrganizationIdAndClientId(org.getId(), client.getId()).isEmpty()) {
                String tan = "BLRP" + (client.getPan() != null ? client.getPan().substring(0, 5) : "12345") + "A";
                if (tdsProfileRepository.findByOrganizationIdAndTan(org.getId(), tan).isEmpty()) {
                    TdsProfileEntity tdsProfile = TdsProfileEntity.builder()
                            .clientId(client.getId())
                            .tan(tan)
                            .deductorType(TdsProfileEntity.DeductorType.COMPANY)
                            .responsiblePersonName(client.getContactPersonName() != null ? client.getContactPersonName() : "Finance Director")
                            .responsiblePersonPan(client.getPan())
                            .responsiblePersonDesignation("Director")
                            .responsiblePersonEmail(client.getEmail())
                            .responsiblePersonMobile(client.getPhone())
                            .status(TdsProfileEntity.TdsProfileStatus.ACTIVE)
                            .tracesStatus(TdsProfileEntity.TracesStatus.REGISTERED_ACTIVE)
                            .build();
                    tdsProfile.setOrganizationId(org.getId());
                    TdsProfileEntity savedTdsProfile = tdsProfileRepository.save(tdsProfile);

                    TdsChallanEntity challan = TdsChallanEntity.builder()
                            .tdsProfileId(savedTdsProfile.getId())
                            .bsrCode("0510304")
                            .challanDate(LocalDate.of(2026, 7, 7))
                            .challanSerialNo("00101")
                            .cin("05103042026070700101")
                            .majorHead(TdsChallanEntity.MajorHead.HEAD_0021_NON_COMPANY)
                            .minorHead(TdsChallanEntity.MinorHead.HEAD_200_PAYABLE_BY_TAXPAYER)
                            .sectionCode("194C")
                            .tdsAmount(new BigDecimal("35000.00"))
                            .totalAmount(new BigDecimal("35000.00"))
                            .utilizedAmount(new BigDecimal("35000.00"))
                            .balanceAmount(BigDecimal.ZERO)
                            .challanStatus(TdsChallanEntity.ChallanStatus.FULLY_UTILIZED)
                            .quarter(TdsQuarter.Q1)
                            .financialYear("2026-27")
                            .build();
                    challan.setOrganizationId(org.getId());
                    tdsChallanRepository.save(challan);

                    TdsReturnEntity tds26q = TdsReturnEntity.builder()
                            .clientId(client.getId())
                            .tdsProfileId(savedTdsProfile.getId())
                            .formType(TdsFormType.FORM_26Q)
                            .quarter(TdsQuarter.Q1)
                            .financialYear("2026-27")
                            .assessmentYear("2027-28")
                            .dueDate(LocalDate.of(2026, 7, 31))
                            .filingStatus(TdsFilingStatus.FILED)
                            .filingDate(LocalDate.of(2026, 7, 28))
                            .tokenNumber("010020304050601")
                            .totalAmountPaid(new BigDecimal("1750000.00"))
                            .totalTaxDeducted(new BigDecimal("35000.00"))
                            .totalTaxDeposited(new BigDecimal("35000.00"))
                            .fvuValidationStatus(FvuValidationStatus.VALIDATED)
                            .notes("Quarterly statement processed via NSDL e-Gov.")
                            .build();
                    tds26q.setOrganizationId(org.getId());
                    tdsReturnRepository.save(tds26q);
                }
            }
        }
    }

    private void seedDemoMarketplaceProfile(OrganizationEntity org) {
        boolean isMundeshwari = org.getName().toUpperCase().contains("MUNDESHWARI");

        if (marketplaceProfileRepository.findByOrganizationId(org.getId()).isEmpty()) {
            MarketplaceProfileEntity profile = MarketplaceProfileEntity.builder()
                    .organizationId(org.getId())
                    .displayName(isMundeshwari ? "Maa Mundeshwari Tax & Legal Consultancy" : "Apex Corporate & Tax Advisors LLP")
                    .slug(isMundeshwari ? "maa-mundeshwari-tax-consultancy" : "apex-corporate-tax-advisors")
                    .headline(isMundeshwari ? "Premier Tax Advocates & High Court Representation" : "Expert Chartered Accountants & Strategic Corporate Tax Advisors")
                    .bio(isMundeshwari
                            ? "Maa Mundeshwari Tax Consultancy is a boutique tax advocacy firm providing end-to-end direct tax litigation, appellate representation before ITAT, GST compliance, and dispute resolution for MSMEs and corporate enterprises."
                            : "Apex Corporate & Tax Advisors LLP is a leading Chartered Accountancy firm delivering high-impact corporate tax planning, international transfer pricing, statutory audits, GST advisory, and startup financial structuring.")
                    .professionalType(isMundeshwari ? MarketplaceProfileEntity.ProfessionalType.TAX_ADVOCATE : MarketplaceProfileEntity.ProfessionalType.CHARTERED_ACCOUNTANT)
                    .experienceYears(isMundeshwari ? 14 : 16)
                    .city(isMundeshwari ? "Patna" : "Bangalore")
                    .state(isMundeshwari ? "Bihar" : "Karnataka")
                    .pincode(isMundeshwari ? "800001" : "560001")
                    .address(isMundeshwari ? "Suite 304, Maurya Lok Complex, Dak Bungalow Road" : "Level 7, Prestige Meridian, MG Road")
                    .phone(isMundeshwari ? "+919835012345" : "+919886012345")
                    .email(isMundeshwari ? "contact@maamundeshwari.com" : "contact@apextax.com")
                    .websiteUrl(isMundeshwari ? "https://maamundeshwari.com" : "https://apextax.com")
                    .specializations("Corporate Tax, GST Compliance, Tax Litigation, Transfer Pricing, Statutory Audit")
                    .languagesSpoken("English, Hindi, Kannada")
                    .startingFee(new BigDecimal("1499.00"))
                    .hourlyRate(new BigDecimal("2999.00"))
                    .averageRating(new BigDecimal("4.95"))
                    .totalReviews(28)
                    .totalClientsServed(150)
                    .visibilityStatus(MarketplaceProfileEntity.VisibilityStatus.PUBLIC)
                    .verificationStatus(MarketplaceProfileEntity.VerificationStatus.VERIFIED)
                    .isPublished(true)
                    .isFeatured(true)
                    .consultationEnabled(true)
                    .consultationFee(new BigDecimal("799.00"))
                    .consultationDurationMinutes(30)
                    .build();

            MarketplaceProfileEntity savedProfile = marketplaceProfileRepository.save(profile);
            log.info("Seeded demo marketplace profile: {} ({})", savedProfile.getDisplayName(), savedProfile.getSlug());

            // Seed sample service packages
            if (marketplaceServiceRepository.findByOrganizationId(org.getId()).isEmpty()) {
                MarketplaceServiceEntity s1 = MarketplaceServiceEntity.builder()
                        .organizationId(org.getId())
                        .marketplaceProfileId(savedProfile.getId())
                        .title("Corporate ITR-6 Filing & Tax Audit (Form 3CD)")
                        .category("ITR")
                        .description("Comprehensive corporate tax computation, MAT calculation, deferred tax provisioning, and statutory Form 3CD tax audit report.")
                        .price(new BigDecimal("14999.00"))
                        .pricingType(MarketplaceServiceEntity.PricingType.FIXED)
                        .deliveryDays(7)
                        .deliverables("Computation Sheet, Form 3CD Audit, ITR-6 Filing Ack, Advisory Call")
                        .isActive(true)
                        .build();
                marketplaceServiceRepository.save(s1);

                MarketplaceServiceEntity s2 = MarketplaceServiceEntity.builder()
                        .organizationId(org.getId())
                        .marketplaceProfileId(savedProfile.getId())
                        .title("Monthly GST Compliance & ITC 2B Reconciliation")
                        .category("GST")
                        .description("Automated monthly GSTR-1, GSTR-3B filings with deep AI-assisted Purchase Register vs GSTR-2B ITC matching.")
                        .price(new BigDecimal("3499.00"))
                        .pricingType(MarketplaceServiceEntity.PricingType.MONTHLY_RETAINER)
                        .deliveryDays(3)
                        .deliverables("GSTR-1, GSTR-3B, ITC mismatch report, e-way bill advisory")
                        .isActive(true)
                        .build();
                marketplaceServiceRepository.save(s2);

                MarketplaceServiceEntity s3 = MarketplaceServiceEntity.builder()
                        .organizationId(org.getId())
                        .marketplaceProfileId(savedProfile.getId())
                        .title("Transfer Pricing & International Tax Advisory")
                        .category("ADVISORY")
                        .description("Benchmarking analysis, Form 3CEB filing, cross-border withholding tax (Form 15CA/CB) certification.")
                        .price(new BigDecimal("24999.00"))
                        .pricingType(MarketplaceServiceEntity.PricingType.FIXED)
                        .deliveryDays(10)
                        .deliverables("Form 3CEB, TP Study Documentation, 15CA/CB Certificate")
                        .isActive(true)
                        .build();
                marketplaceServiceRepository.save(s3);
            }

            // Seed sample inbound customer leads
            if (marketplaceLeadRepository.findAllByOrganizationId(org.getId()).isEmpty()) {
                MarketplaceLeadEntity lead1 = MarketplaceLeadEntity.builder()
                        .organizationId(org.getId())
                        .marketplaceProfileId(savedProfile.getId())
                        .clientName("Siddharth Nambiar")
                        .clientEmail("siddharth.n@zenittech.io")
                        .clientPhone("+919812345670")
                        .serviceCategory("GST & Corporate Tax Advisory")
                        .requirementDescription("Looking for an experienced CA firm to manage GST return filing and annual ROC compliance for our SaaS tech startup.")
                        .budgetRange("₹25,000 - ₹50,000")
                        .urgency(MarketplaceLeadEntity.Urgency.STANDARD)
                        .leadStatus(MarketplaceLeadEntity.LeadStatus.NEW)
                        .build();
                marketplaceLeadRepository.save(lead1);

                MarketplaceLeadEntity lead2 = MarketplaceLeadEntity.builder()
                        .organizationId(org.getId())
                        .marketplaceProfileId(savedProfile.getId())
                        .clientName("Meenakshi Sundaram")
                        .clientEmail("meenakshi@innovatelegal.in")
                        .clientPhone("+919876541230")
                        .serviceCategory("Statutory Tax Audit (Form 3CD)")
                        .requirementDescription("Private limited enterprise requiring FY 2025-26 statutory balance sheet finalization and Form 3CD tax audit certification.")
                        .budgetRange("₹45,000 - ₹75,000")
                        .urgency(MarketplaceLeadEntity.Urgency.URGENT)
                        .leadStatus(MarketplaceLeadEntity.LeadStatus.CONTACTED)
                        .build();
                marketplaceLeadRepository.save(lead2);
            }
        }
    }
}
