package com.taxoryn.module.authentication.service;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.authentication.dto.LoginResponse;
import com.taxoryn.module.authentication.dto.LogoutRequest;
import com.taxoryn.module.authentication.dto.RefreshTokenRequest;
import com.taxoryn.module.authentication.dto.RegisterOrganizationRequest;
import com.taxoryn.module.authentication.dto.RegisterUserByAdminRequest;
import com.taxoryn.module.organization.dto.OrganizationDto;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationEntity.SubscriptionPlan;
import com.taxoryn.module.organization.mapper.OrganizationMapper;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.role.service.RoleService;
import com.taxoryn.module.user.dto.UserDto;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.mapper.UserMapper;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;
    private final com.taxoryn.module.subscription.service.SubscriptionService subscriptionService;

    @Value("${taxoryn.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE, "User account is " + user.getStatus() + ". Please contact administrator");
        }

        if (user.getOrganizationId() != null) {
            OrganizationEntity organization = organizationRepository.findById(user.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", user.getOrganizationId()));

            if (organization.getStatus() != OrganizationStatus.ACTIVE) {
                throw new AppException(ErrorCode.ACCOUNT_INACTIVE, "Organization account is " + organization.getStatus() + ". Access suspended");
            }

            log.info("User {} successfully logged in for organization {}", user.getId(), organization.getId());
            return createAuthResponse(user, organization);
        } else {
            log.info("Marketplace customer user {} successfully logged in", user.getId());
            return createAuthResponse(user, null);
        }
    }

    @Override
    @Transactional
    public LoginResponse registerOrganization(RegisterOrganizationRequest request) {
        String orgEmail = request.getOrganizationEmail().toLowerCase().trim();
        String adminEmail = request.getAdminEmail().toLowerCase().trim();

        if (organizationRepository.existsByEmailIgnoreCase(orgEmail)) {
            throw new DuplicateResourceException("Organization", "email", orgEmail);
        }

        if (userRepository.findByEmailIgnoreCase(adminEmail).isPresent()) {
            throw new DuplicateResourceException("User", "email", adminEmail);
        }

        // 1. Create Organization Entity
        OrganizationEntity organization = OrganizationEntity.builder()
                .name(request.getOrganizationName().trim())
                .email(orgEmail)
                .phone(request.getOrganizationPhone())
                .pan(request.getPan())
                .gstin(request.getGstin())
                .status(OrganizationStatus.ACTIVE)
                .subscriptionPlan(SubscriptionPlan.STARTER)
                .build();

        OrganizationEntity savedOrg = organizationRepository.save(organization);
        log.info("Created organization tenant: id={}, name={}", savedOrg.getId(), savedOrg.getName());

        // 2. Fetch ORG_ADMIN Role
        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> {
                    RoleEntity fallback = RoleEntity.builder()
                            .code("ORG_ADMIN")
                            .name("Organization Administrator")
                            .isSystemRole(true)
                            .build();
                    return roleRepository.save(fallback);
                });

        // 3. Create Admin User under the new Tenant
        UserEntity adminUser = UserEntity.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(request.getAdminPassword()))
                .firstName(request.getAdminFirstName().trim())
                .lastName(request.getAdminLastName() != null ? request.getAdminLastName().trim() : null)
                .phone(request.getAdminPhone())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        adminUser.setOrganizationId(savedOrg.getId());

        UserEntity savedUser = userRepository.save(adminUser);
        log.info("Created initial admin user: id={}, email={} for tenant={}", savedUser.getId(), savedUser.getEmail(), savedOrg.getId());

        // 4. Create Initial STARTER SaaS Subscription
        subscriptionService.createInitialSubscription(savedOrg.getId(), com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan.STARTER);

        return createAuthResponse(savedUser, savedOrg);
    }

    @Override
    @Transactional
    public UserDto registerUserByAdmin(RegisterUserByAdminRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        // Check MAX_USERS Subscription Limit
        subscriptionService.checkUserLimit(organizationId);

        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new DuplicateResourceException("User", "email", email);
        }

        List<RoleEntity> roles = roleService.getRolesByCodes(request.getRoleCodes(), organizationId);

        UserEntity user = UserEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName() != null ? request.getLastName().trim() : null)
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(roles))
                .build();
        user.setOrganizationId(organizationId);

        UserEntity saved = userRepository.save(user);
        log.info("Admin registered team user: id={}, email={} under tenant={}", saved.getId(), saved.getEmail(), organizationId);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!jwtTokenProvider.validateToken(token)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
        UUID organizationId = jwtTokenProvider.getOrganizationIdFromToken(token);

        UserEntity user;
        if (organizationId != null) {
            user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                    .orElseThrow(() -> new UnauthorizedException("User not found for refresh token"));
        } else {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new UnauthorizedException("User not found for refresh token"));
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE, "User account is " + user.getStatus());
        }

        OrganizationEntity organization = null;
        if (organizationId != null) {
            organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new UnauthorizedException("Organization not found for refresh token"));

            if (organization.getStatus() != OrganizationStatus.ACTIVE) {
                throw new AppException(ErrorCode.ACCOUNT_INACTIVE, "Organization account is " + organization.getStatus());
            }
        }

        return createAuthResponse(user, organization);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getMe() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID organizationId = SecurityUtils.getCurrentUser().map(SecurityUser::getOrganizationId).orElse(null);

        UserEntity user;
        if (organizationId != null) {
            user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        } else {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        return userMapper.toDto(user);
    }

    @Override
    public void logout(String authHeader, LogoutRequest request) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7).trim();
            jwtTokenProvider.invalidateToken(accessToken);
        }

        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            jwtTokenProvider.invalidateToken(request.getRefreshToken().trim());
        }

        log.info("User successfully logged out and tokens invalidated");
    }

    private LoginResponse createAuthResponse(UserEntity user, OrganizationEntity organization) {
        Set<String> roleCodes = user.getRoles().stream()
                .map(RoleEntity::getCode)
                .collect(Collectors.toSet());

        Set<String> permissionCodes = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(PermissionEntity::getCode)
                .collect(Collectors.toSet());

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                organization != null ? organization.getId() : null,
                user.getClientId(),
                user.getEmail(),
                roleCodes,
                permissionCodes
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(),
                organization != null ? organization.getId() : null,
                user.getClientId(),
                user.getEmail()
        );

        UserDto userDto = userMapper.toDto(user);
        OrganizationDto orgDto = organization != null ? organizationMapper.toDto(organization) : null;

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .user(userDto)
                .organization(orgDto)
                .build();
    }
}
