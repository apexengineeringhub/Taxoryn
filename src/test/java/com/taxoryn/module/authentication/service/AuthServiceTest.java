package com.taxoryn.module.authentication.service;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.authentication.dto.LoginResponse;
import com.taxoryn.module.authentication.dto.LogoutRequest;
import com.taxoryn.module.authentication.dto.RefreshTokenRequest;
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

import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.authentication.entity.RefreshTokenEntity;
import com.taxoryn.module.authentication.repository.PasswordResetTokenRepository;
import com.taxoryn.module.authentication.repository.RefreshTokenRepository;
import com.taxoryn.module.notification.email.service.EmailNotificationService;
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

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
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

    @Mock
    private com.taxoryn.module.subscription.service.SubscriptionService subscriptionService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID userId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 86400000L);
        ReflectionTestUtils.setField(authService, "jwtRefreshExpirationMs", 604800000L);
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
        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any(), any(), any()))
                .thenReturn("mock.access.token");
        when(userMapper.toDto(user)).thenReturn(UserDto.builder().id(userId).email(email).build());
        when(organizationMapper.toDto(org)).thenReturn(OrganizationDto.builder().id(tenantId).name("Test Practice").build());

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(rawPassword)
                .build();

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock.access.token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
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

        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any(), any(), any())).thenReturn("access.jwt");

        LoginResponse response = authService.registerOrganization(request);

        assertNotNull(response);
        assertEquals("access.jwt", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
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
    @DisplayName("SECURITY: Refresh token rotation succeeds and issues child token in same family")
    void testRefreshToken_Success() {
        UUID familyId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        String rawToken = "valid-raw-refresh-token-1234567890";

        RefreshTokenEntity tokenEntity = RefreshTokenEntity.builder()
                .id(tokenId)
                .userId(userId)
                .organizationId(tenantId)
                .familyId(familyId)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        UserEntity user = UserEntity.builder()
                .email("user@taxoryn.com")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(RoleEntity.builder().code("ORG_ADMIN").permissions(new HashSet<>()).build())))
                .build();
        user.setId(userId);
        user.setOrganizationId(tenantId);

        OrganizationEntity org = OrganizationEntity.builder()
                .name("Practice")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build();
        org.setId(tenantId);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenEntity));
        when(refreshTokenRepository.revokeSingleTokenAtomic(eq(tokenId), any(), eq("ROTATED"))).thenReturn(1);
        when(userRepository.findByIdAndOrganizationId(userId, tenantId)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(tenantId)).thenReturn(Optional.of(org));
        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any(), any(), any())).thenReturn("new.access.token");

        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(rawToken).build();
        LoginResponse response = authService.refreshToken(request, "127.0.0.1", "JUnit-Agent");

        assertNotNull(response);
        assertEquals("new.access.token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(refreshTokenRepository, atLeastOnce()).save(any(RefreshTokenEntity.class));
    }

    @Test
    @DisplayName("SECURITY: Reuse detection invalidates entire token family")
    void testRefreshToken_ReuseDetected_RevokesEntireFamily() {
        UUID familyId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        String rawToken = "stolen-old-token-already-rotated";

        RefreshTokenEntity revokedToken = RefreshTokenEntity.builder()
                .id(tokenId)
                .userId(userId)
                .organizationId(tenantId)
                .familyId(familyId)
                .revokedAt(Instant.now().minusSeconds(60))
                .revokedReason("ROTATED")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedToken));

        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(rawToken).build();

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));
        verify(refreshTokenRepository).revokeAllByFamilyId(eq(familyId), any(), eq("REUSE_DETECTED"));
    }

    @Test
    @DisplayName("SECURITY: Expired refresh token throws UnauthorizedException")
    void testRefreshToken_Expired_ThrowsUnauthorized() {
        UUID tokenId = UUID.randomUUID();
        RefreshTokenEntity expiredToken = RefreshTokenEntity.builder()
                .id(tokenId)
                .userId(userId)
                .organizationId(tenantId)
                .familyId(UUID.randomUUID())
                .expiresAt(Instant.now().minusSeconds(100))
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("expired-token").build();

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));
    }

    @Test
    @DisplayName("SECURITY: Concurrent collision on single-use refresh token triggers family revocation")
    void testRefreshToken_ConcurrentCollision_TriggersFamilyRevocation() {
        UUID familyId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        RefreshTokenEntity tokenEntity = RefreshTokenEntity.builder()
                .id(tokenId)
                .userId(userId)
                .organizationId(tenantId)
                .familyId(familyId)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenEntity));
        // Atomic update returns 0 indicating another concurrent thread already updated the row
        when(refreshTokenRepository.revokeSingleTokenAtomic(eq(tokenId), any(), eq("ROTATED"))).thenReturn(0);

        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("concurrent-token").build();

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));
        verify(refreshTokenRepository).revokeAllByFamilyId(eq(familyId), any(), eq("REUSE_DETECTED"));
    }

    @Test
    @DisplayName("SECURITY: Inactive user cannot refresh tokens and family is revoked")
    void testRefreshToken_DisabledUser_RevokesFamily() {
        UUID familyId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        RefreshTokenEntity tokenEntity = RefreshTokenEntity.builder()
                .id(tokenId)
                .userId(userId)
                .organizationId(tenantId)
                .familyId(familyId)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        UserEntity user = UserEntity.builder()
                .email("disabled@taxoryn.com")
                .status(UserEntity.UserStatus.SUSPENDED)
                .roles(new HashSet<>())
                .build();
        user.setId(userId);
        user.setOrganizationId(tenantId);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenEntity));
        when(refreshTokenRepository.revokeSingleTokenAtomic(eq(tokenId), any(), eq("ROTATED"))).thenReturn(1);
        when(userRepository.findByIdAndOrganizationId(userId, tenantId)).thenReturn(Optional.of(user));

        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("token").build();

        assertThrows(AppException.class, () -> authService.refreshToken(request));
        verify(refreshTokenRepository).revokeAllByFamilyId(eq(familyId), any(), eq("ACCOUNT_INACTIVE"));
    }

    @Test
    @DisplayName("Logout invalidates access token and refresh token family")
    void testLogout() {
        String authHeader = "Bearer some.jwt.token";
        String rawRefresh = "some.refresh.token";
        LogoutRequest logoutRequest = new LogoutRequest(rawRefresh);

        UUID familyId = UUID.randomUUID();
        RefreshTokenEntity tokenEntity = RefreshTokenEntity.builder()
                .familyId(familyId)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenEntity));

        authService.logout(authHeader, logoutRequest);

        verify(jwtTokenProvider).invalidateToken("some.jwt.token");
        verify(refreshTokenRepository).revokeAllByFamilyId(eq(familyId), any(), eq("LOGOUT"));
    }

    @Test
    @DisplayName("Logout all sessions invalidates all tokens for user")
    void testLogoutAllSessions() {
        authService.logoutAllSessions();
        verify(refreshTokenRepository).revokeAllByUserId(eq(userId), any(), eq("LOGOUT_ALL"));
    }
}
