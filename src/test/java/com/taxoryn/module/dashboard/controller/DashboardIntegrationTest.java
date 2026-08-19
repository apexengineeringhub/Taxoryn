package com.taxoryn.module.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gst.entity.GstProfileEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstReturnType;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.itr.entity.ItrProfileEntity;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private GstProfileRepository gstProfileRepository;

    @Autowired
    private GstReturnFilingRepository gstReturnFilingRepository;

    @Autowired
    private ItrProfileRepository itrProfileRepository;

    @Autowired
    private ItrReturnRepository itrReturnRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity tenantA;
    private OrganizationEntity tenantB;
    private UserEntity userA;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        taskRepository.deleteAll();
        gstReturnFilingRepository.deleteAll();
        gstProfileRepository.deleteAll();
        itrReturnRepository.deleteAll();
        itrProfileRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseGet(() -> {
            RoleEntity r = RoleEntity.builder()
                    .code("ORG_ADMIN")
                    .name("Organization Administrator")
                    .isSystemRole(true)
                    .build();
            return roleRepository.save(r);
        });

        // 1. Setup Tenant A
        tenantA = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant A Practice " + UUID.randomUUID())
                .legalName("Tenant A LLP")
                .email("adminA." + UUID.randomUUID() + "@taxpractice.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(tenantA.getId());
        userA = UserEntity.builder()
                .email(tenantA.getEmail())
                .passwordHash(passwordEncoder.encode("SecretPassword123!"))
                .firstName("Admin")
                .lastName("TenantA")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build();
        userA.setOrganizationId(tenantA.getId());
        userA = userRepository.save(userA);

        tokenA = jwtTokenProvider.generateAccessToken(
                userA.getId(), tenantA.getId(), null, userA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("DASHBOARD_VIEW", "CLIENT_VIEW")
        );

        // 2. Setup Tenant B
        tenantB = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant B Competitor " + UUID.randomUUID())
                .legalName("Tenant B LLP")
                .email("adminB." + UUID.randomUUID() + "@taxpractice.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(tenantB.getId());
        UserEntity userB = UserEntity.builder()
                .email(tenantB.getEmail())
                .passwordHash(passwordEncoder.encode("SecretPassword123!"))
                .firstName("Admin")
                .lastName("TenantB")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build();
        userB.setOrganizationId(tenantB.getId());
        userB = userRepository.save(userB);

        tokenB = jwtTokenProvider.generateAccessToken(
                userB.getId(), tenantB.getId(), null, userB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("DASHBOARD_VIEW", "CLIENT_VIEW")
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("GET /api/v1/dashboard should return complete organization metrics with tenant isolation")
    void testGetOrganizationDashboard() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        // 1. Clients for Tenant A (1 ACTIVE, 1 INACTIVE)
        ClientEntity client1 = ClientEntity.builder()
                .displayName("Alpha Corp")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .pan("AAACB1111A")
                .build();
        client1.setOrganizationId(tenantA.getId());
        client1 = clientRepository.save(client1);

        ClientEntity client2 = ClientEntity.builder()
                .displayName("Beta Corp")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.INACTIVE)
                .pan("BBBCB2222B")
                .build();
        client2.setOrganizationId(tenantA.getId());
        client2 = clientRepository.save(client2);

        // 2. Employee for Tenant A
        EmployeeEntity employeeA = EmployeeEntity.builder()
                .employeeCode("EMP-101")
                .firstName("Rohan")
                .lastName("Verma")
                .email("rohan.v@tenantA.com")
                .department("Tax")
                .designation("Associate")
                .status(EmployeeStatus.ACTIVE)
                .build();
        employeeA.setOrganizationId(tenantA.getId());
        employeeA = employeeRepository.save(employeeA);

        // 3. Tasks for Tenant A (1 COMPLETED, 1 TODO overdue)
        TaskEntity task1 = TaskEntity.builder()
                .clientId(client1.getId())
                .assignedTo(employeeA.getId())
                .title("Prepare GSTR-1")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.COMPLETED)
                .dueDate(LocalDate.now().minusDays(5))
                .build();
        task1.setOrganizationId(tenantA.getId());
        taskRepository.save(task1);

        TaskEntity task2 = TaskEntity.builder()
                .clientId(client1.getId())
                .assignedTo(employeeA.getId())
                .title("File ITR-6")
                .priority(TaskPriority.URGENT)
                .status(TaskStatus.TODO)
                .dueDate(LocalDate.now().minusDays(2)) // overdue
                .build();
        task2.setOrganizationId(tenantA.getId());
        taskRepository.save(task2);

        // 4. GST for Tenant A
        GstProfileEntity gstProfile = GstProfileEntity.builder()
                .clientId(client1.getId())
                .gstin("27AAACB1111A1Z5")
                .legalName("Alpha Corp Legal")
                .build();
        gstProfile.setOrganizationId(tenantA.getId());
        gstProfile = gstProfileRepository.save(gstProfile);

        GstReturnFilingEntity gstFiling = GstReturnFilingEntity.builder()
                .clientId(client1.getId())
                .gstProfileId(gstProfile.getId())
                .returnType(GstReturnType.GSTR1)
                .returnPeriod("2026-07")
                .financialYear("2026-27")
                .dueDate(LocalDate.now().plusDays(10))
                .filingStatus(GstFilingStatus.FILED)
                .build();
        gstFiling.setOrganizationId(tenantA.getId());
        gstReturnFilingRepository.save(gstFiling);

        // 5. ITR for Tenant A
        ItrProfileEntity itrProfile = ItrProfileEntity.builder()
                .clientId(client1.getId())
                .pan("AAACB1111A")
                .taxpayerType(TaxpayerType.COMPANY)
                .defaultItrType(ItrType.ITR_6)
                .build();
        itrProfile.setOrganizationId(tenantA.getId());
        itrProfile = itrProfileRepository.save(itrProfile);

        ItrReturnEntity itrReturn = ItrReturnEntity.builder()
                .clientId(client1.getId())
                .itrProfileId(itrProfile.getId())
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .itrType(ItrType.ITR_6)
                .taxpayerType(TaxpayerType.COMPANY)
                .dueDate(LocalDate.now().plusDays(30))
                .status(ItrStatus.DATA_ENTRY) // pending
                .build();
        itrReturn.setOrganizationId(tenantA.getId());
        itrReturnRepository.save(itrReturn);

        // 6. Billing for Tenant A
        InvoiceEntity invoice = InvoiceEntity.builder()
                .clientId(client1.getId())
                .invoiceNumber("INV-2026-0001")
                .invoiceDate(LocalDate.now().minusDays(10))
                .dueDate(LocalDate.now().plusDays(20))
                .status(InvoiceStatus.PARTIALLY_PAID)
                .subtotal(new BigDecimal("8474.58"))
                .tax(new BigDecimal("1525.42"))
                .total(new BigDecimal("10000.00"))
                .paidAmount(new BigDecimal("6000.00"))
                .balanceDue(new BigDecimal("4000.00"))
                .build();
        invoice.setOrganizationId(tenantA.getId());
        invoiceRepository.save(invoice);

        // 7. Seed data in Tenant B (should NEVER bleed into Tenant A)
        TenantContext.setTenantId(tenantB.getId());
        ClientEntity clientB = ClientEntity.builder()
                .displayName("Competitor Client")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .pan("ZZZCB9999Z")
                .build();
        clientB.setOrganizationId(tenantB.getId());
        clientRepository.save(clientB);

        InvoiceEntity invoiceB = InvoiceEntity.builder()
                .clientId(clientB.getId())
                .invoiceNumber("INV-2026-9999")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(InvoiceStatus.ISSUED)
                .subtotal(new BigDecimal("42372.88"))
                .tax(new BigDecimal("7627.12"))
                .total(new BigDecimal("50000.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("50000.00"))
                .build();
        invoiceB.setOrganizationId(tenantB.getId());
        invoiceRepository.save(invoiceB);

        // Execute GET /api/v1/dashboard as Tenant A
        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // Clients: 2 total, 1 active, 1 inactive
                .andExpect(jsonPath("$.data.clients.total").value(2))
                .andExpect(jsonPath("$.data.clients.active").value(1))
                .andExpect(jsonPath("$.data.clients.inactive").value(1))
                // Employees: 1 total, 1 active
                .andExpect(jsonPath("$.data.employees.total").value(1))
                .andExpect(jsonPath("$.data.employees.active").value(1))
                // Tasks: 2 total, 1 pending, 1 overdue, 1 completed
                .andExpect(jsonPath("$.data.tasks.total").value(2))
                .andExpect(jsonPath("$.data.tasks.pending").value(1))
                .andExpect(jsonPath("$.data.tasks.overdue").value(1))
                .andExpect(jsonPath("$.data.tasks.completed").value(1))
                // GST: 1 client, 1 filed, 0 due, 0 overdue
                .andExpect(jsonPath("$.data.gst.totalGstClients").value(1))
                .andExpect(jsonPath("$.data.gst.returnsFiled").value(1))
                .andExpect(jsonPath("$.data.gst.returnsDue").value(0))
                // ITR: 1 client, 1 pending, 0 filed, 0 overdue
                .andExpect(jsonPath("$.data.itr.totalItrClients").value(1))
                .andExpect(jsonPath("$.data.itr.pending").value(1))
                .andExpect(jsonPath("$.data.itr.filed").value(0))
                // Billing: total 10000, paid 6000, balance 4000
                .andExpect(jsonPath("$.data.billing.totalInvoiceAmount").value(10000.00))
                .andExpect(jsonPath("$.data.billing.paidAmount").value(6000.00))
                .andExpect(jsonPath("$.data.billing.outstandingAmount").value(4000.00))
                // Employee workload
                .andExpect(jsonPath("$.data.employeeWorkload", hasSize(1)))
                .andExpect(jsonPath("$.data.employeeWorkload[0].employeeCode").value("EMP-101"))
                .andExpect(jsonPath("$.data.employeeWorkload[0].assignedTasks").value(2))
                .andExpect(jsonPath("$.data.employeeWorkload[0].pendingTasks").value(1))
                .andExpect(jsonPath("$.data.employeeWorkload[0].overdueTasks").value(1));

        // Test route alias /api/dashboard and /api/dashboard/organization
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clients.total").value(2));

        mockMvc.perform(get("/api/dashboard/organization")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clients.total").value(2));
    }
}
