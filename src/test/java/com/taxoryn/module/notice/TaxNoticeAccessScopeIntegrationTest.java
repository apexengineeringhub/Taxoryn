package com.taxoryn.module.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.notice.dto.UpdateTaxNoticeRequest;
import com.taxoryn.module.notice.entity.TaxNoticeEntity;
import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.notice.enums.NoticeRisk;
import com.taxoryn.module.notice.enums.NoticeStatus;
import com.taxoryn.module.notice.repository.NoticeActivityRepository;
import com.taxoryn.module.notice.repository.NoticeHearingRepository;
import com.taxoryn.module.notice.repository.NoticeResponseRepository;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.repository.WorkItemRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.entity.UserLocationEntity;
import com.taxoryn.module.user.repository.UserLocationRepository;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TaxNoticeAccessScopeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaxNoticeRepository noticeRepository;

    @Autowired
    private NoticeResponseRepository responseRepository;

    @Autowired
    private NoticeHearingRepository hearingRepository;

    @Autowired
    private NoticeActivityRepository activityRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserLocationRepository userLocationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private LocationEntity locMumbai;
    private LocationEntity locDelhi;
    private UserEntity firmAdmin;
    private UserEntity staffMumbai;
    private EmployeeEntity empMumbai;
    private ClientEntity clientMumbai;
    private ClientEntity clientDelhi;
    private TaxNoticeEntity noticeMumbai;
    private TaxNoticeEntity noticeDelhi;
    private TaxNoticeEntity noticeOrg2;

    private String firmAdminToken;
    private String staffMumbaiToken;

    @BeforeEach
    void setUp() {
        cleanDb();

        // Org 1
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Consultants Org 1")
                .email("admin@apexorg1.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // Org 2 (for multi-tenant isolation testing)
        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Other Tax Firm Org 2")
                .email("admin@otherorg2.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(org1.getId());

        // Locations in Org 1
        locMumbai = locationRepository.save(LocationEntity.builder()
                .code("MUM-01")
                .name("Mumbai Branch")
                .city("Mumbai")
                .state("Maharashtra")
                .isHeadOffice(true)
                .isActive(true)
                .build());

        locDelhi = locationRepository.save(LocationEntity.builder()
                .code("DEL-01")
                .name("Delhi Branch")
                .city("Delhi")
                .state("Delhi")
                .isHeadOffice(false)
                .isActive(true)
                .build());

        // Roles
        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .name("Admin Role")
                .code("ORG_ADMIN")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity staffRole = roleRepository.save(RoleEntity.builder()
                .name("Staff Role")
                .code("STAFF")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // Users
        firmAdmin = userRepository.save(UserEntity.builder()
                .email("firmadmin-" + UUID.randomUUID() + "@apex.com")
                .passwordHash(passwordEncoder.encode("Secure123!"))
                .firstName("Apex")
                .lastName("FirmAdmin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        staffMumbai = userRepository.save(UserEntity.builder()
                .email("mumbaistaff-" + UUID.randomUUID() + "@apex.com")
                .passwordHash(passwordEncoder.encode("Secure123!"))
                .firstName("Mumbai")
                .lastName("Associate")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());

        empMumbai = employeeRepository.save(EmployeeEntity.builder()
                .userId(staffMumbai.getId())
                .employeeCode("EMP-MUM-01")
                .firstName("Mumbai")
                .lastName("Associate")
                .email(staffMumbai.getEmail())
                .status(EmployeeStatus.ACTIVE)
                .build());

        // Scope staffMumbai to Mumbai location
        userLocationRepository.save(UserLocationEntity.builder()
                .id(UserLocationEntity.UserLocationId.builder()
                        .userId(staffMumbai.getId())
                        .locationId(locMumbai.getId())
                        .build())
                .organizationId(org1.getId())
                .build());

        // Clients in Org 1
        clientMumbai = clientRepository.save(ClientEntity.builder()
                .displayName("Mumbai Trading Co")
                .legalName("Mumbai Trading Company")
                .pan("MUMBA1234K")
                .clientType(ClientType.COMPANY)
                .locationId(locMumbai.getId())
                .status(ClientStatus.ACTIVE)
                .build());

        clientDelhi = clientRepository.save(ClientEntity.builder()
                .displayName("Delhi Retail Enterprises")
                .legalName("Delhi Retail Enterprises Ltd")
                .pan("DELHI1234K")
                .clientType(ClientType.COMPANY)
                .locationId(locDelhi.getId())
                .status(ClientStatus.ACTIVE)
                .build());

        // Notices in Org 1
        noticeMumbai = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(clientMumbai.getId())
                .locationId(locMumbai.getId())
                .noticeNumber("NOT-MUM-" + UUID.randomUUID().toString().substring(0, 6))
                .department(NoticeDepartment.GST)
                .noticeType("GST ASMT-10 Discrepancy")
                .subject("GST Mismatch in GSTR-1 vs 3B")
                .status(NoticeStatus.RECEIVED)
                .priority(NoticePriority.HIGH)
                .riskLevel(NoticeRisk.MEDIUM)
                .receivedDate(LocalDate.now().minusDays(2))
                .responseDueDate(LocalDate.now().plusDays(15))
                .build());

        noticeDelhi = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(clientDelhi.getId())
                .locationId(locDelhi.getId())
                .noticeNumber("NOT-DEL-" + UUID.randomUUID().toString().substring(0, 6))
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("ITR Demand Notice 156")
                .subject("Outstanding Tax Demand for AY 2024-25")
                .status(NoticeStatus.RECEIVED)
                .priority(NoticePriority.CRITICAL)
                .riskLevel(NoticeRisk.HIGH)
                .receivedDate(LocalDate.now().minusDays(2))
                .responseDueDate(LocalDate.now().plusDays(5))
                .build());

        // Client and Notice in Org 2
        TenantContext.setTenantId(org2.getId());
        ClientEntity clientOrg2 = clientRepository.save(ClientEntity.builder()
                .displayName("Org2 Client Private Limited")
                .legalName("Org2 Client Private Limited")
                .pan("ORG021234K")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .build());

        noticeOrg2 = noticeRepository.save(TaxNoticeEntity.builder()
                .clientId(clientOrg2.getId())
                .noticeNumber("NOT-ORG2-" + UUID.randomUUID().toString().substring(0, 6))
                .department(NoticeDepartment.TDS)
                .noticeType("TDS Short Deduction u/s 201")
                .subject("TDS Demand on Late Deduction")
                .status(NoticeStatus.RECEIVED)
                .priority(NoticePriority.MEDIUM)
                .riskLevel(NoticeRisk.LOW)
                .receivedDate(LocalDate.now().minusDays(2))
                .responseDueDate(LocalDate.now().plusDays(30))
                .build());

        TenantContext.setTenantId(org1.getId());
        Set<String> noticePermissions = Set.of("NOTICE_VIEW", "NOTICE_CREATE", "NOTICE_UPDATE", "NOTICE_DELETE");
        firmAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(firmAdmin.getId(), org1.getId(), firmAdmin.getEmail(), Set.of("ORG_ADMIN"), noticePermissions);
        staffMumbaiToken = "Bearer " + jwtTokenProvider.generateAccessToken(staffMumbai.getId(), org1.getId(), staffMumbai.getEmail(), Set.of("STAFF"), noticePermissions);
    }

    @AfterEach
    void tearDown() {
        cleanDb();
        TenantContext.clear();
    }

    private void cleanDb() {
        TenantContext.clear();
        activityRepository.deleteAll();
        hearingRepository.deleteAll();
        responseRepository.deleteAll();
        workItemRepository.deleteAll();
        taskRepository.deleteAll();
        noticeRepository.deleteAll();
        clientRepository.deleteAll();
        userLocationRepository.deleteAll();
        employeeRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Admin has full visibility across all locations in organization")
    void testAdminHasFullVisibilityAcrossLocations() throws Exception {
        mockMvc.perform(get("/api/v1/notices")
                        .header("Authorization", firmAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        mockMvc.perform(get("/api/v1/notices/" + noticeMumbai.getId())
                        .header("Authorization", firmAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(noticeMumbai.getId().toString()));

        mockMvc.perform(get("/api/v1/notices/" + noticeDelhi.getId())
                        .header("Authorization", firmAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(noticeDelhi.getId().toString()));
    }

    @Test
    @DisplayName("Location Scoped User: Only sees notices in assigned location and receives 403 for other locations")
    void testLocationScopedUserAccessControl() throws Exception {
        // Staff Mumbai listing notices should only see noticeMumbai
        mockMvc.perform(get("/api/v1/notices")
                        .header("Authorization", staffMumbaiToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(noticeMumbai.getId().toString()));

        // Staff Mumbai accessing noticeMumbai -> 200 OK
        mockMvc.perform(get("/api/v1/notices/" + noticeMumbai.getId())
                        .header("Authorization", staffMumbaiToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(noticeMumbai.getId().toString()));

        // Staff Mumbai accessing noticeDelhi -> 403 Forbidden
        mockMvc.perform(get("/api/v1/notices/" + noticeDelhi.getId())
                        .header("Authorization", staffMumbaiToken))
                .andExpect(status().isForbidden());

        // Staff Mumbai trying to update noticeDelhi -> 403 Forbidden
        UpdateTaxNoticeRequest updateRequest = UpdateTaxNoticeRequest.builder()
                .subject("Unauthorized update attempt")
                .build();

        mockMvc.perform(put("/api/v1/notices/" + noticeDelhi.getId())
                        .header("Authorization", staffMumbaiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Multi-Tenant Isolation: User in Org 1 cannot access notice in Org 2 (returns 404)")
    void testTenantIsolationCrossOrgAccessReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/notices/" + noticeOrg2.getId())
                        .header("Authorization", firmAdminToken))
                .andExpect(status().isNotFound());
    }
}
