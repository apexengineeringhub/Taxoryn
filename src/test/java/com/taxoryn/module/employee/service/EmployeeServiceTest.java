package com.taxoryn.module.employee.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.employee.dto.CreateEmployeeRequest;
import com.taxoryn.module.employee.dto.EmployeeDto;
import com.taxoryn.module.employee.dto.EmployeeWorkloadDto;
import com.taxoryn.module.employee.dto.UpdateEmployeeRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeStatusRequest;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.mapper.EmployeeMapper;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
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

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private com.taxoryn.module.role.repository.RoleRepository roleRepository;

    @Mock
    private com.taxoryn.module.organization.repository.OrganizationRepository organizationRepository;

    @Mock
    private com.taxoryn.module.authentication.repository.OrganizationActivationTokenRepository organizationActivationTokenRepository;

    @Mock
    private com.taxoryn.module.notification.email.service.EmailNotificationService emailNotificationService;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private com.taxoryn.module.authentication.repository.RefreshTokenRepository refreshTokenRepository;

    @Mock
    private com.taxoryn.module.audit.service.AuditService auditService;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private UUID tenantId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        employeeId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(tenantId)
                .email("admin@taxpractice.com")
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("EMPLOYEE_CREATE", "EMPLOYEE_VIEW", "EMPLOYEE_UPDATE"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Create employee successfully")
    void testCreateEmployeeSuccess() {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .employeeCode("EMP-001")
                .firstName("Rohan")
                .lastName("Deshmukh")
                .email("rohan.d@taxpractice.com")
                .phone("+919876543210")
                .department("Taxation")
                .designation("Senior Associate")
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2024, 1, 15))
                .build();

        when(employeeRepository.existsByOrganizationIdAndEmployeeCode(tenantId, "EMP-001")).thenReturn(false);
        when(employeeRepository.existsByOrganizationIdAndEmail(tenantId, "rohan.d@taxpractice.com")).thenReturn(false);

        EmployeeEntity saved = EmployeeEntity.builder()
                .employeeCode("EMP-001")
                .firstName("Rohan")
                .lastName("Deshmukh")
                .email("rohan.d@taxpractice.com")
                .phone("+919876543210")
                .department("Taxation")
                .designation("Senior Associate")
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2024, 1, 15))
                .build();
        saved.setId(employeeId);
        saved.setOrganizationId(tenantId);

        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        when(userRepository.save(any())).thenAnswer(invocation -> {
            com.taxoryn.module.user.entity.UserEntity u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(userRepository.findById(any())).thenAnswer(invocation -> {
            com.taxoryn.module.user.entity.UserEntity u = com.taxoryn.module.user.entity.UserEntity.builder()
                    .email("rohan.d@taxpractice.com")
                    .firstName("Rohan")
                    .lastName("Deshmukh")
                    .status(com.taxoryn.module.user.entity.UserEntity.UserStatus.INACTIVE)
                    .build();
            u.setId(invocation.getArgument(0));
            return Optional.of(u);
        });

        when(employeeRepository.save(any(EmployeeEntity.class))).thenReturn(saved);
        when(employeeMapper.toDto(saved)).thenReturn(EmployeeDto.builder()
                .id(employeeId)
                .employeeCode("EMP-001")
                .firstName("Rohan")
                .lastName("Deshmukh")
                .email("rohan.d@taxpractice.com")
                .department("Taxation")
                .designation("Senior Associate")
                .status(EmployeeStatus.ACTIVE)
                .build());

        EmployeeDto result = employeeService.createEmployee(request);

        assertNotNull(result);
        assertEquals("EMP-001", result.getEmployeeCode());
        assertEquals("Rohan Deshmukh", result.getFullName());
        verify(organizationActivationTokenRepository).save(any());
        verify(emailNotificationService).sendEmployeeInvitationEmail(eq("rohan.d@taxpractice.com"), eq("Rohan Deshmukh"), any(), eq("Senior Associate"), any(), eq(24L));
    }

    @Test
    @DisplayName("Resend employee invitation dispatches fresh token and email")
    void testResendInvitationSuccess() {
        EmployeeEntity employee = EmployeeEntity.builder()
                .employeeCode("EMP-001")
                .firstName("Rohan")
                .lastName("Deshmukh")
                .email("rohan.d@taxpractice.com")
                .designation("Senior Associate")
                .userId(UUID.randomUUID())
                .build();
        employee.setId(employeeId);
        employee.setOrganizationId(tenantId);

        com.taxoryn.module.user.entity.UserEntity user = com.taxoryn.module.user.entity.UserEntity.builder()
                .email("rohan.d@taxpractice.com")
                .firstName("Rohan")
                .lastName("Deshmukh")
                .status(com.taxoryn.module.user.entity.UserEntity.UserStatus.INACTIVE)
                .build();
        user.setId(employee.getUserId());
        user.setOrganizationId(tenantId);

        when(employeeRepository.findByIdAndOrganizationId(employeeId, tenantId)).thenReturn(Optional.of(employee));
        when(userRepository.findByIdAndOrganizationId(employee.getUserId(), tenantId)).thenReturn(Optional.of(user));

        employeeService.resendInvitation(employeeId);

        verify(organizationActivationTokenRepository).invalidateAllPendingTokensForUser(eq(user.getId()), any());
        verify(organizationActivationTokenRepository).save(any());
        verify(emailNotificationService).sendEmployeeInvitationEmail(eq("rohan.d@taxpractice.com"), eq("Rohan Deshmukh"), any(), eq("Senior Associate"), any(), eq(24L));
    }

    @Test
    @DisplayName("Create employee fails on duplicate employee code")
    void testCreateEmployeeDuplicateCodeThrows() {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .employeeCode("EMP-001")
                .firstName("Rohan")
                .email("rohan.d@taxpractice.com")
                .department("Taxation")
                .designation("Senior Associate")
                .build();

        when(employeeRepository.existsByOrganizationIdAndEmployeeCode(tenantId, "EMP-001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(request));
    }

    @Test
    @DisplayName("Update employee self-manager assignment fails")
    void testUpdateEmployeeSelfManagerAssignmentFails() {
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .firstName("Rohan")
                .email("rohan.d@taxpractice.com")
                .department("Taxation")
                .designation("Senior Associate")
                .managerId(employeeId) // Self manager assignment
                .build();

        EmployeeEntity employee = EmployeeEntity.builder()
                .firstName("Rohan")
                .email("rohan.d@taxpractice.com")
                .build();
        employee.setId(employeeId);
        employee.setOrganizationId(tenantId);

        when(employeeRepository.findByIdAndOrganizationId(employeeId, tenantId)).thenReturn(Optional.of(employee));

        assertThrows(BusinessValidationException.class, () -> employeeService.updateEmployee(employeeId, request));
    }

    @Test
    @DisplayName("Get employee workload calculates tasks accurately")
    void testGetEmployeeWorkload() {
        EmployeeEntity employee = EmployeeEntity.builder()
                .employeeCode("EMP-001")
                .firstName("Rohan")
                .lastName("Deshmukh")
                .userId(UUID.randomUUID())
                .build();
        employee.setId(employeeId);
        employee.setOrganizationId(tenantId);

        when(employeeRepository.findByIdAndOrganizationId(employeeId, tenantId)).thenReturn(Optional.of(employee));
        when(taskRepository.countAssignedTasks(eq(tenantId), any())).thenReturn(10L);
        when(taskRepository.countByStatuses(eq(tenantId), any(), eq(Set.of(TaskStatus.COMPLETED)))).thenReturn(4L);
        when(taskRepository.countByStatuses(eq(tenantId), any(), eq(Set.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.UNDER_REVIEW)))).thenReturn(6L);
        when(taskRepository.countOverdueTasks(eq(tenantId), any(), eq(Set.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.UNDER_REVIEW)), any(LocalDate.class))).thenReturn(2L);

        EmployeeWorkloadDto workload = employeeService.getEmployeeWorkload(employeeId);

        assertNotNull(workload);
        assertEquals(employeeId, workload.getEmployeeId());
        assertEquals(10L, workload.getTotalAssignedTasks());
        assertEquals(6L, workload.getPendingTasks());
        assertEquals(2L, workload.getOverdueTasks());
        assertEquals(4L, workload.getCompletedTasks());
    }

    @Test
    @DisplayName("Update employee status to SUSPENDED revokes tokens and sets user status")
    void testUpdateEmployeeStatusToSuspended_RevokesTokensAndSetsUserStatus() {
        UUID targetUserId = UUID.randomUUID();
        EmployeeEntity employee = EmployeeEntity.builder()
                .firstName("Rohan")
                .userId(targetUserId)
                .status(EmployeeStatus.ACTIVE)
                .build();
        employee.setId(employeeId);
        employee.setOrganizationId(tenantId);

        com.taxoryn.module.user.entity.UserEntity user = com.taxoryn.module.user.entity.UserEntity.builder()
                .email("rohan@practice.com")
                .status(com.taxoryn.module.user.entity.UserEntity.UserStatus.ACTIVE)
                .build();
        user.setId(targetUserId);
        user.setOrganizationId(tenantId);

        when(employeeRepository.findByIdAndOrganizationId(employeeId, tenantId)).thenReturn(Optional.of(employee));
        when(userRepository.findByIdAndOrganizationId(targetUserId, tenantId)).thenReturn(Optional.of(user));
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toDto(employee)).thenReturn(EmployeeDto.builder().id(employeeId).status(EmployeeStatus.SUSPENDED).build());

        EmployeeDto result = employeeService.updateEmployeeStatus(employeeId, new UpdateEmployeeStatusRequest(EmployeeStatus.SUSPENDED));

        assertNotNull(result);
        assertEquals(EmployeeStatus.SUSPENDED, result.getStatus());
        assertEquals(com.taxoryn.module.user.entity.UserEntity.UserStatus.SUSPENDED, user.getStatus());
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUserId(eq(targetUserId), any(), eq("ACCOUNT_SUSPENDED"));
    }

    @Test
    @DisplayName("Update employee status to INACTIVE revokes tokens and sets user status")
    void testUpdateEmployeeStatusToInactive_RevokesTokensAndSetsUserStatus() {
        UUID targetUserId = UUID.randomUUID();
        EmployeeEntity employee = EmployeeEntity.builder()
                .firstName("Rohan")
                .userId(targetUserId)
                .status(EmployeeStatus.ACTIVE)
                .build();
        employee.setId(employeeId);
        employee.setOrganizationId(tenantId);

        com.taxoryn.module.user.entity.UserEntity user = com.taxoryn.module.user.entity.UserEntity.builder()
                .email("rohan@practice.com")
                .status(com.taxoryn.module.user.entity.UserEntity.UserStatus.ACTIVE)
                .build();
        user.setId(targetUserId);
        user.setOrganizationId(tenantId);

        when(employeeRepository.findByIdAndOrganizationId(employeeId, tenantId)).thenReturn(Optional.of(employee));
        when(userRepository.findByIdAndOrganizationId(targetUserId, tenantId)).thenReturn(Optional.of(user));
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toDto(employee)).thenReturn(EmployeeDto.builder().id(employeeId).status(EmployeeStatus.INACTIVE).build());

        EmployeeDto result = employeeService.updateEmployeeStatus(employeeId, new UpdateEmployeeStatusRequest(EmployeeStatus.INACTIVE));

        assertNotNull(result);
        assertEquals(EmployeeStatus.INACTIVE, result.getStatus());
        assertEquals(com.taxoryn.module.user.entity.UserEntity.UserStatus.INACTIVE, user.getStatus());
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUserId(eq(targetUserId), any(), eq("ACCOUNT_DEACTIVATED"));
    }

    @Test
    @DisplayName("Update employee status to ACTIVE reactivates user")
    void testUpdateEmployeeStatusToActive_ReactivatesUser() {
        UUID targetUserId = UUID.randomUUID();
        EmployeeEntity employee = EmployeeEntity.builder()
                .firstName("Rohan")
                .userId(targetUserId)
                .status(EmployeeStatus.SUSPENDED)
                .build();
        employee.setId(employeeId);
        employee.setOrganizationId(tenantId);

        com.taxoryn.module.user.entity.UserEntity user = com.taxoryn.module.user.entity.UserEntity.builder()
                .email("rohan@practice.com")
                .status(com.taxoryn.module.user.entity.UserEntity.UserStatus.SUSPENDED)
                .build();
        user.setId(targetUserId);
        user.setOrganizationId(tenantId);

        when(employeeRepository.findByIdAndOrganizationId(employeeId, tenantId)).thenReturn(Optional.of(employee));
        when(userRepository.findByIdAndOrganizationId(targetUserId, tenantId)).thenReturn(Optional.of(user));
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toDto(employee)).thenReturn(EmployeeDto.builder().id(employeeId).status(EmployeeStatus.ACTIVE).build());

        EmployeeDto result = employeeService.updateEmployeeStatus(employeeId, new UpdateEmployeeStatusRequest(EmployeeStatus.ACTIVE));

        assertNotNull(result);
        assertEquals(EmployeeStatus.ACTIVE, result.getStatus());
        assertEquals(com.taxoryn.module.user.entity.UserEntity.UserStatus.ACTIVE, user.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Self status update is blocked")
    void testUpdateEmployeeStatus_SelfUpdateBlocked() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        EmployeeEntity employee = EmployeeEntity.builder()
                .firstName("Admin")
                .userId(currentUserId)
                .status(EmployeeStatus.ACTIVE)
                .build();
        employee.setId(employeeId);
        employee.setOrganizationId(tenantId);

        when(employeeRepository.findByIdAndOrganizationId(employeeId, tenantId)).thenReturn(Optional.of(employee));

        assertThrows(BusinessValidationException.class, () ->
                employeeService.updateEmployeeStatus(employeeId, new UpdateEmployeeStatusRequest(EmployeeStatus.SUSPENDED)));
    }

    @Test
    @DisplayName("Last active practice admin protection prevents suspension")
    void testUpdateEmployeeStatus_LastActiveAdminProtection() {
        UUID targetUserId = UUID.randomUUID();
        EmployeeEntity employee = EmployeeEntity.builder()
                .firstName("Sole Admin")
                .userId(targetUserId)
                .status(EmployeeStatus.ACTIVE)
                .build();
        employee.setId(employeeId);
        employee.setOrganizationId(tenantId);

        com.taxoryn.module.role.entity.RoleEntity adminRole = com.taxoryn.module.role.entity.RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Administrator")
                .build();

        com.taxoryn.module.user.entity.UserEntity user = com.taxoryn.module.user.entity.UserEntity.builder()
                .email("soleadmin@practice.com")
                .status(com.taxoryn.module.user.entity.UserEntity.UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build();
        user.setId(targetUserId);
        user.setOrganizationId(tenantId);

        when(employeeRepository.findByIdAndOrganizationId(employeeId, tenantId)).thenReturn(Optional.of(employee));
        when(userRepository.findByIdAndOrganizationId(targetUserId, tenantId)).thenReturn(Optional.of(user));
        when(userRepository.countActiveOrgAdmins(tenantId)).thenReturn(1L);

        assertThrows(BusinessValidationException.class, () ->
                employeeService.updateEmployeeStatus(employeeId, new UpdateEmployeeStatusRequest(EmployeeStatus.SUSPENDED)));
    }
}
