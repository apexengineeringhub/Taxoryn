package com.taxoryn.module.employee.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.employee.dto.CreateEmployeeRequest;
import com.taxoryn.module.employee.dto.EmployeeDto;
import com.taxoryn.module.employee.dto.EmployeeFilterRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeRequest;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.employee.repository.OrganizationEmployeeCounterRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationScopedEmployeeNumberIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private OrganizationEmployeeNumberGenerator numberGenerator;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationEmployeeCounterRepository counterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity adminUserA;
    private UserEntity adminUserB;
    private String adminTokenA;
    private String adminTokenB;
    private RoleEntity orgAdminRole;
    private RoleEntity taxAssociateRole;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        employeeRepository.deleteAll();
        counterRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Seed Roles
        orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Administrator")
                .isSystemRole(true)
                .build());

        taxAssociateRole = roleRepository.save(RoleEntity.builder()
                .code("TAX_ASSOCIATE")
                .name("Tax Associate")
                .isSystemRole(true)
                .build());

        // 2. Create Organization A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Verma & Co CPAs")
                .email("contact@vermacpa.com")
                .build());

        adminUserA = userRepository.save(UserEntity.builder()
                .email("admin@vermacpa.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Vikram")
                .lastName("Verma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());
        adminUserA.setOrganizationId(orgA.getId());
        adminUserA = userRepository.save(adminUserA);
        adminTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserA.getId(),
                orgA.getId(),
                adminUserA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("EMPLOYEE_VIEW", "EMPLOYEE_CREATE", "EMPLOYEE_UPDATE", "EMPLOYEE_DELETE", "ROLE_READ")
        );

        // 3. Create Organization B
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Consultants")
                .email("contact@apextax.com")
                .build());

        adminUserB = userRepository.save(UserEntity.builder()
                .email("admin@apextax.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Ananya")
                .lastName("Roy")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());
        adminUserB.setOrganizationId(orgB.getId());
        adminUserB = userRepository.save(adminUserB);
        adminTokenB = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserB.getId(),
                orgB.getId(),
                adminUserB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("EMPLOYEE_VIEW", "EMPLOYEE_CREATE", "EMPLOYEE_UPDATE", "EMPLOYEE_DELETE", "ROLE_READ")
        );

        // 4. Initialize Organization Employee Counters
        counterRepository.save(com.taxoryn.module.employee.entity.OrganizationEmployeeCounterEntity.builder()
                .organizationId(orgA.getId())
                .lastNumber(0L)
                .updatedAt(java.time.Instant.now())
                .build());

        counterRepository.save(com.taxoryn.module.employee.entity.OrganizationEmployeeCounterEntity.builder()
                .organizationId(orgB.getId())
                .lastNumber(0L)
                .updatedAt(java.time.Instant.now())
                .build());
    }

    @Test
    @DisplayName("1. First employee in Organization A receives EMP-0001")
    void testFirstEmployeeInOrgA() {
        TenantContext.setTenantId(orgA.getId());

        CreateEmployeeRequest req = CreateEmployeeRequest.builder()
                .firstName("Rohan")
                .lastName("Deshmukh")
                .email("rohan.d@vermacpa.com")
                .department("Taxation")
                .designation("Tax Associate")
                .build();

        EmployeeDto emp = employeeService.createEmployee(req);
        assertThat(emp.getEmployeeCode()).isEqualTo("EMP-0001");
        assertThat(emp.getEmployeeNumber()).isEqualTo("EMP-0001");
        assertThat(emp.getId()).isNotNull();
    }

    @Test
    @DisplayName("2. Second employee in Organization A receives EMP-0002")
    void testSecondEmployeeInOrgA() {
        TenantContext.setTenantId(orgA.getId());

        EmployeeDto emp1 = employeeService.createEmployee(CreateEmployeeRequest.builder()
                .firstName("Rohan")
                .lastName("Deshmukh")
                .email("rohan.d@vermacpa.com")
                .department("Taxation")
                .designation("Tax Associate")
                .build());

        EmployeeDto emp2 = employeeService.createEmployee(CreateEmployeeRequest.builder()
                .firstName("Pooja")
                .lastName("Sharma")
                .email("pooja.s@vermacpa.com")
                .department("Direct Tax")
                .designation("Senior Tax Associate")
                .build());

        assertThat(emp1.getEmployeeCode()).isEqualTo("EMP-0001");
        assertThat(emp2.getEmployeeCode()).isEqualTo("EMP-0002");
    }

    @Test
    @DisplayName("3. First employee in Organization B starts independently at EMP-0001")
    void testFirstEmployeeInOrgBStartsIndependently() {
        // Create 2 employees in Org A
        TenantContext.setTenantId(orgA.getId());
        employeeService.createEmployee(CreateEmployeeRequest.builder()
                .firstName("Rohan")
                .email("rohan@vermacpa.com")
                .department("Taxation")
                .designation("Tax Associate")
                .build());
        employeeService.createEmployee(CreateEmployeeRequest.builder()
                .firstName("Pooja")
                .email("pooja@vermacpa.com")
                .department("Taxation")
                .designation("Tax Associate")
                .build());

        // Switch to Org B
        TenantContext.setTenantId(orgB.getId());
        EmployeeDto empB1 = employeeService.createEmployee(CreateEmployeeRequest.builder()
                .firstName("Suresh")
                .lastName("Patel")
                .email("suresh.p@apextax.com")
                .department("Audit")
                .designation("Audit Associate")
                .build());

        assertThat(empB1.getEmployeeCode()).isEqualTo("EMP-0001");
        assertThat(empB1.getOrganizationId()).isEqualTo(orgB.getId());
    }

    @Test
    @DisplayName("4. Duplicate employee number within same organization is rejected")
    void testDuplicateEmployeeNumberInSameOrgRejected() {
        TenantContext.setTenantId(orgA.getId());

        // Create first with explicit code EMP-0001
        employeeService.createEmployee(CreateEmployeeRequest.builder()
                .employeeCode("EMP-0001")
                .firstName("Rohan")
                .email("rohan@vermacpa.com")
                .department("Taxation")
                .designation("Tax Associate")
                .build());

        // Try creating second with same code EMP-0001
        assertThatThrownBy(() -> employeeService.createEmployee(CreateEmployeeRequest.builder()
                .employeeCode("EMP-0001")
                .firstName("Duplicate")
                .email("duplicate@vermacpa.com")
                .department("Taxation")
                .designation("Tax Associate")
                .build()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("5. Same employee number across different organizations is allowed")
    void testSameEmployeeNumberAcrossDifferentOrgsAllowed() {
        TenantContext.setTenantId(orgA.getId());
        EmployeeDto empA = employeeService.createEmployee(CreateEmployeeRequest.builder()
                .employeeCode("EMP-0001")
                .firstName("Rohan")
                .email("rohan@vermacpa.com")
                .department("Taxation")
                .designation("Tax Associate")
                .build());

        TenantContext.setTenantId(orgB.getId());
        EmployeeDto empB = employeeService.createEmployee(CreateEmployeeRequest.builder()
                .employeeCode("EMP-0001")
                .firstName("Suresh")
                .email("suresh@apextax.com")
                .department("Audit")
                .designation("Audit Associate")
                .build());

        assertThat(empA.getEmployeeCode()).isEqualTo("EMP-0001");
        assertThat(empB.getEmployeeCode()).isEqualTo("EMP-0001");
        assertThat(empA.getId()).isNotEqualTo(empB.getId());
        assertThat(empA.getOrganizationId()).isEqualTo(orgA.getId());
        assertThat(empB.getOrganizationId()).isEqualTo(orgB.getId());
    }

    @Test
    @DisplayName("6. Employee number persists after update")
    void testEmployeeNumberPersistsAfterUpdate() {
        TenantContext.setTenantId(orgA.getId());
        EmployeeDto created = employeeService.createEmployee(CreateEmployeeRequest.builder()
                .firstName("Rohan")
                .lastName("Deshmukh")
                .email("rohan.d@vermacpa.com")
                .department("Taxation")
                .designation("Tax Associate")
                .build());

        assertThat(created.getEmployeeCode()).isEqualTo("EMP-0001");

        UpdateEmployeeRequest updateReq = UpdateEmployeeRequest.builder()
                .firstName("Rohan")
                .lastName("Deshmukh-Senior")
                .email("rohan.senior@vermacpa.com")
                .department("Senior Taxation")
                .designation("Senior Tax Consultant")
                .build();

        EmployeeDto updated = employeeService.updateEmployee(created.getId(), updateReq);
        assertThat(updated.getEmployeeCode()).isEqualTo("EMP-0001");
        assertThat(updated.getEmployeeNumber()).isEqualTo("EMP-0001");
        assertThat(updated.getLastName()).isEqualTo("Deshmukh-Senior");
    }

    @Test
    @DisplayName("7. Employee number search works via API")
    void testEmployeeNumberSearch() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        employeeService.createEmployee(CreateEmployeeRequest.builder()
                .firstName("Rohan")
                .email("rohan@vermacpa.com")
                .department("Taxation")
                .designation("Tax Associate")
                .build());

        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", adminTokenA)
                        .param("search", "EMP-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].employeeCode").value("EMP-0001"))
                .andExpect(jsonPath("$.data.content[0].employeeNumber").value("EMP-0001"))
                .andExpect(jsonPath("$.data.content[0].firstName").value("Rohan"));
    }

    @Test
    @DisplayName("8. Cross-tenant employee lookup fails with 404")
    void testCrossTenantEmployeeLookupFails() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        EmployeeDto empA = employeeService.createEmployee(CreateEmployeeRequest.builder()
                .firstName("Rohan")
                .email("rohan@vermacpa.com")
                .department("Taxation")
                .designation("Tax Associate")
                .build());

        // Admin B attempts to access Employee from Org A
        mockMvc.perform(get("/api/v1/employees/" + empA.getId())
                        .header("Authorization", adminTokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("9. Bulk onboarding uses organization-scoped sequence")
    void testBulkOnboardingUsesOrganizationSequence() {
        TenantContext.setTenantId(orgA.getId());

        List<CreateEmployeeRequest> bulkList = List.of(
                CreateEmployeeRequest.builder().firstName("Staff1").email("staff1@vermacpa.com").department("Tax").designation("Associate").build(),
                CreateEmployeeRequest.builder().firstName("Staff2").email("staff2@vermacpa.com").department("Tax").designation("Associate").build(),
                CreateEmployeeRequest.builder().firstName("Staff3").email("staff3@vermacpa.com").department("Tax").designation("Associate").build(),
                CreateEmployeeRequest.builder().firstName("Staff4").email("staff4@vermacpa.com").department("Tax").designation("Associate").build(),
                CreateEmployeeRequest.builder().firstName("Staff5").email("staff5@vermacpa.com").department("Tax").designation("Associate").build()
        );

        var result = employeeService.bulkCreateEmployees(bulkList);
        assertThat(result.getTotalCreated()).isEqualTo(5);
        List<String> codes = result.getCreatedEmployees().stream().map(EmployeeDto::getEmployeeCode).toList();
        assertThat(codes).containsExactly("EMP-0001", "EMP-0002", "EMP-0003", "EMP-0004", "EMP-0005");
    }

    @Test
    @DisplayName("10. Concurrency test: 20 simultaneous creations in Org A receive unique gapless EMP-0001..EMP-0020")
    void testConcurrentEmployeeCreationsUniqueSequence() throws Exception {
        int count = 20;
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(count);

        Set<String> generatedCodes = ConcurrentHashMap.newKeySet();
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 1; i <= count; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TenantContext.setTenantId(orgA.getId());
                    EmployeeDto dto = employeeService.createEmployee(CreateEmployeeRequest.builder()
                            .firstName("Staff" + index)
                            .lastName("Worker")
                            .email("concurrent.staff." + index + "@vermacpa.com")
                            .department("Taxation")
                            .designation("Tax Associate")
                            .build());
                    generatedCodes.add(dto.getEmployeeCode());
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Trigger all threads concurrently
        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(errors).isEmpty();
        assertThat(generatedCodes).hasSize(count);

        // Verify all codes from EMP-0001 to EMP-0020 exist
        for (int i = 1; i <= count; i++) {
            String expected = String.format("EMP-%04d", i);
            assertThat(generatedCodes).contains(expected);
        }
    }

    @Test
    @DisplayName("11. Concurrency test across multiple organizations runs in parallel without crosstalk")
    void testConcurrencyAcrossOrganizations() throws Exception {
        int countPerOrg = 10;
        ExecutorService executor = Executors.newFixedThreadPool(countPerOrg * 2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(countPerOrg * 2);

        Set<String> orgACodes = ConcurrentHashMap.newKeySet();
        Set<String> orgBCodes = ConcurrentHashMap.newKeySet();
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 1; i <= countPerOrg; i++) {
            final int idx = i;
            // Org A worker
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TenantContext.setTenantId(orgA.getId());
                    EmployeeDto dto = employeeService.createEmployee(CreateEmployeeRequest.builder()
                            .firstName("OrgAStaff" + idx)
                            .email("orga.staff." + idx + "@vermacpa.com")
                            .department("Taxation")
                            .designation("Tax Associate")
                            .build());
                    orgACodes.add(dto.getEmployeeCode());
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    endLatch.countDown();
                }
            });

            // Org B worker
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TenantContext.setTenantId(orgB.getId());
                    EmployeeDto dto = employeeService.createEmployee(CreateEmployeeRequest.builder()
                            .firstName("OrgBStaff" + idx)
                            .email("orgb.staff." + idx + "@apextax.com")
                            .department("Audit")
                            .designation("Audit Associate")
                            .build());
                    orgBCodes.add(dto.getEmployeeCode());
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(errors).isEmpty();
        assertThat(orgACodes).hasSize(countPerOrg);
        assertThat(orgBCodes).hasSize(countPerOrg);

        for (int i = 1; i <= countPerOrg; i++) {
            String expected = String.format("EMP-%04d", i);
            assertThat(orgACodes).contains(expected);
            assertThat(orgBCodes).contains(expected);
        }
    }
}
