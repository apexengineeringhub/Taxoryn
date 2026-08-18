package com.taxoryn.module.authentication.service;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.authentication.dto.LoginResponse;
import com.taxoryn.module.authentication.dto.LogoutRequest;
import com.taxoryn.module.authentication.dto.RegisterOrganizationRequest;
import com.taxoryn.module.authentication.dto.RegisterUserByAdminRequest;
import com.taxoryn.module.organization.dto.OrganizationDto;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.mapper.OrganizationMapper;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.role.service.RoleService;
import com.taxoryn.module.user.dto.UserDto;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.mapper.UserMapper;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrganizationMapper organizationMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID userId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 86400000L);
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .organizationId(tenantId)
                .email("admin@taxoryn.com")
                .roles(Set.of("ORG_ADMIN"))
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
    @DisplayName("Login successful with valid credentials")
    void testLoginSuccess() {
        String email = "test@taxoryn.com";
        String rawPassword = "Password123!";
        String encodedPassword = "$2a$12$encodedPassword";

        RoleEntity orgAdminRole = RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Admin")
                .permissions(new HashSet<>())
                .build();

        UserEntity user = UserEntity.builder()
                .email(email)
                .passwordHash(encodedPassword)
                .firstName("Test")
                .lastName("User")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        user.setId(userId);
        user.setOrganizationId(tenantId);

        OrganizationEntity org = OrganizationEntity.builder()
                .name("Test Practice")
                .email("org@taxoryn.com")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build();
        org.setId(tenantId);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(organizationRepository.findById(tenantId)).thenReturn(Optional.of(org));
        when(jwtTokenProvider.generateAccessToken(eq(userId), eq(tenantId), eq(email), any(), any()))
                .thenReturn("mock.access.token");
        when(jwtTokenProvider.generateRefreshToken(eq(userId), eq(tenantId), eq(email)))
                .thenReturn("mock.refresh.token");
        when(userMapper.toDto(user)).thenReturn(UserDto.builder().id(userId).email(email).build());
        when(organizationMapper.toDto(org)).thenReturn(OrganizationDto.builder().id(tenantId).name("Test Practice").build());

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(rawPassword)
                .build();

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock.access.token", response.getAccessToken());
        assertEquals("mock.refresh.token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    @DisplayName("Login fails with invalid password throws UnauthorizedException")
    void testLoginInvalidPassword() {
        String email = "test@taxoryn.com";
        UserEntity user = UserEntity.builder()
                .email(email)
                .passwordHash("hashed")
                .status(UserEntity.UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed")).thenReturn(false);

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password("WrongPassword")
                .build();

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Login fails for disabled/inactive user throws AppException")
    void testLoginDisabledUser() {
        String email = "inactive@taxoryn.com";
        UserEntity user = UserEntity.builder()
                .email(email)
                .passwordHash("hashed")
                .status(UserEntity.UserStatus.INACTIVE)
                .build();

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashed")).thenReturn(true);

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password("Password123!")
                .build();

        assertThrows(AppException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Register Organization creates organization, admin user, and returns tokens")
    void testRegisterOrganizationSuccess() {
        UUID orgId = UUID.randomUUID();
        UUID newUserId = UUID.randomUUID();

        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("Sharma Tax Solutions")
                .organizationEmail("contact@sharmatax.com")
                .adminFirstName("Rajesh")
                .adminLastName("Sharma")
                .adminEmail("rajesh@sharmatax.com")
                .adminPassword("SecurePass123!")
                .build();

        when(organizationRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        OrganizationEntity savedOrg = OrganizationEntity.builder()
                .name(request.getOrganizationName())
                .email(request.getOrganizationEmail())
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build();
        savedOrg.setId(orgId);
        when(organizationRepository.save(any(OrganizationEntity.class))).thenReturn(savedOrg);

        RoleEntity role = RoleEntity.builder().code("ORG_ADMIN").permissions(new HashSet<>()).build();
        when(roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encoded");

        UserEntity savedUser = UserEntity.builder()
                .email(request.getAdminEmail())
                .firstName(request.getAdminFirstName())
                .roles(new HashSet<>(Set.of(role)))
                .status(UserEntity.UserStatus.ACTIVE)
                .build();
        savedUser.setId(newUserId);
        savedUser.setOrganizationId(orgId);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any(), any())).thenReturn("access.jwt");
        when(jwtTokenProvider.generateRefreshToken(any(), any(), any())).thenReturn("refresh.jwt");

        LoginResponse response = authService.registerOrganization(request);

        assertNotNull(response);
        assertEquals("access.jwt", response.getAccessToken());
        assertEquals("refresh.jwt", response.getRefreshToken());
    }

    @Test
    @DisplayName("Register user by admin saves user under admin's tenant")
    void testRegisterUserByAdmin() {
        RegisterUserByAdminRequest request = RegisterUserByAdminRequest.builder()
                .firstName("Anil")
                .lastName("Kumar")
                .email("anil@taxoryn.com")
                .password("Password123!")
                .roleCodes(Set.of("ASSOCIATE"))
                .build();

        when(userRepository.findByEmailIgnoreCase(request.getEmail())).thenReturn(Optional.empty());
        RoleEntity role = RoleEntity.builder().code("ASSOCIATE").permissions(new HashSet<>()).build();
        when(roleService.getRolesByCodes(request.getRoleCodes(), tenantId)).thenReturn(List.of(role));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("$2a$12$encoded");

        UserEntity savedUser = UserEntity.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(role)))
                .build();
        savedUser.setId(UUID.randomUUID());
        savedUser.setOrganizationId(tenantId);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(UserDto.builder().id(savedUser.getId()).email(request.getEmail()).build());

        UserDto result = authService.registerUserByAdmin(request);

        assertNotNull(result);
        assertEquals(request.getEmail(), result.getEmail());
    }

    @Test
    @DisplayName("Logout invalidates access token and refresh token")
    void testLogout() {
        String authHeader = "Bearer some.jwt.token";
        LogoutRequest logoutRequest = new LogoutRequest("some.refresh.token");

        authService.logout(authHeader, logoutRequest);

        verify(jwtTokenProvider).invalidateToken("some.jwt.token");
        verify(jwtTokenProvider).invalidateToken("some.refresh.token");
    }
}
