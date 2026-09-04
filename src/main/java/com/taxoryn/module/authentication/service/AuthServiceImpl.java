package com.taxoryn.module.authentication.service;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.authentication.dto.ChangePasswordRequest;
import com.taxoryn.module.authentication.dto.ForgotPasswordRequest;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.authentication.dto.LoginResponse;
import com.taxoryn.module.authentication.dto.LogoutRequest;
import com.taxoryn.module.authentication.dto.RefreshTokenRequest;
import com.taxoryn.module.authentication.dto.RegisterOrganizationRequest;
import com.taxoryn.module.authentication.dto.RegisterUserByAdminRequest;
import com.taxoryn.module.authentication.dto.ResetPasswordRequest;
import com.taxoryn.module.authentication.entity.PasswordResetTokenEntity;
import com.taxoryn.module.authentication.entity.RefreshTokenEntity;
import com.taxoryn.module.authentication.repository.PasswordResetTokenRepository;
import com.taxoryn.module.authentication.repository.RefreshTokenRepository;
import com.taxoryn.module.notification.email.service.EmailNotificationService;
import com.taxoryn.module.organization.dto.OrganizationDto;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationEntity.SubscriptionPlan;
import com.taxoryn.module.organization.mapper.OrganizationMapper;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import org.springframework.scheduling.annotation.Scheduled;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.taxoryn.module.notification.whatsapp.event.UserRegisteredEvent;
import com.taxoryn.module.notification.whatsapp.event.UserRegistrationType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailNotificationService emailNotificationService;
    private final AuditService auditService;

    @Value("${taxoryn.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Value("${taxoryn.jwt.refresh-expiration-ms:604800000}")
    private long jwtRefreshExpirationMs;

    @Value("${taxoryn.auth.password-reset.expiration-minutes:30}")
    private long passwordResetExpirationMinutes;

    @Value("${taxoryn.frontend.reset-password-url:${taxoryn.auth.reset-password-base-url:${TAXORYN_RESET_PASSWORD_URL:https://taxoryn.com/reset-password}}}")
    private String resetPasswordBaseUrl;

    @Override
    @Transactional
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

        String phone = org.springframework.util.StringUtils.hasText(request.getAdminPhone())
                ? request.getAdminPhone().trim()
                : (org.springframework.util.StringUtils.hasText(request.getOrganizationPhone()) ? request.getOrganizationPhone().trim() : null);

        // 3. Create Admin User under the new Tenant
        UserEntity adminUser = UserEntity.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(request.getAdminPassword()))
                .firstName(request.getAdminFirstName().trim())
                .lastName(request.getAdminLastName() != null ? request.getAdminLastName().trim() : null)
                .phone(phone)
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        adminUser.setOrganizationId(savedOrg.getId());

        UserEntity savedUser = userRepository.save(adminUser);
        log.info("Created initial admin user: id={}, email={} for tenant={}", savedUser.getId(), savedUser.getEmail(), savedOrg.getId());

        // 4. Create Initial STARTER SaaS Subscription
        subscriptionService.createInitialSubscription(savedOrg.getId(), com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan.STARTER);

        // 5. Publish UserRegisteredEvent for post-registration welcome notifications
        eventPublisher.publishEvent(UserRegisteredEvent.builder()
                .userId(savedUser.getId())
                .organizationId(savedOrg.getId())
                .registrationType(UserRegistrationType.PRACTITIONER)
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .organizationName(savedOrg.getName())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .build());

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

        // RBAC Privilege Escalation & Delegation Boundary Check
        SecurityUtils.validateRoleDelegation(request.getRoleCodes(), null);

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
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        return refreshToken(request, null, null);
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request, String clientIp, String userAgent) {
        String rawToken = request.getRefreshToken();
        if (!StringUtils.hasText(rawToken)) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String tokenHash = hashToken(rawToken.trim());
        RefreshTokenEntity tokenEntity = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        // 1. REUSE DETECTION: If token was already revoked, an attacker or compromised client is replaying an old token!
        if (tokenEntity.isRevoked()) {
            log.warn("SECURITY ALERT: Refresh token reuse detected for familyId: {}, userId: {}. Revoking entire token family.",
                    tokenEntity.getFamilyId(), tokenEntity.getUserId());

            // Invalidate ALL refresh tokens belonging to this session family immediately
            refreshTokenRepository.revokeAllByFamilyId(tokenEntity.getFamilyId(), Instant.now(), "REUSE_DETECTED");

            auditService.logEvent(
                    tokenEntity.getOrganizationId(),
                    tokenEntity.getUserId(),
                    "REFRESH_TOKEN_REUSE_DETECTED",
                    "REFRESH_TOKEN",
                    tokenEntity.getId().toString(),
                    null,
                    "Revoked token reused. Entire session family invalidated."
            );

            throw new UnauthorizedException("Refresh token reuse detected. Session terminated for security.");
        }

        // 2. EXPIRATION CHECK
        if (tokenEntity.isExpired()) {
            log.info("Refresh token expired for userId: {}", tokenEntity.getUserId());
            tokenEntity.setRevokedAt(Instant.now());
            tokenEntity.setRevokedReason("EXPIRED");
            refreshTokenRepository.save(tokenEntity);
            throw new UnauthorizedException("Refresh token has expired. Please log in again.");
        }

        // 3. ATOMIC ROTATION / CONCURRENCY PROTECTION
        int updated = refreshTokenRepository.revokeSingleTokenAtomic(tokenEntity.getId(), Instant.now(), "ROTATED");
        if (updated == 0) {
            log.warn("Concurrent refresh collision detected for token ID: {}. Triggering family revocation.", tokenEntity.getId());
            refreshTokenRepository.revokeAllByFamilyId(tokenEntity.getFamilyId(), Instant.now(), "REUSE_DETECTED");
            throw new UnauthorizedException("Refresh token already consumed or invalid");
        }

        // 4. VERIFY USER AND TENANT STATUS (Re-derived directly from persistent store)
        UUID userId = tokenEntity.getUserId();
        UUID organizationId = tokenEntity.getOrganizationId();

        UserEntity user;
        if (organizationId != null) {
            user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                    .orElseThrow(() -> new UnauthorizedException("User not found for refresh token"));
        } else {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new UnauthorizedException("User not found for refresh token"));
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            refreshTokenRepository.revokeAllByFamilyId(tokenEntity.getFamilyId(), Instant.now(), "ACCOUNT_INACTIVE");
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE, "User account is " + user.getStatus());
        }

        OrganizationEntity organization = null;
        if (organizationId != null) {
            organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new UnauthorizedException("Organization not found for refresh token"));

            if (organization.getStatus() != OrganizationStatus.ACTIVE) {
                refreshTokenRepository.revokeAllByFamilyId(tokenEntity.getFamilyId(), Instant.now(), "ORGANIZATION_INACTIVE");
                throw new AppException(ErrorCode.ACCOUNT_INACTIVE, "Organization account is " + organization.getStatus());
            }
        }

        // 5. ROTATE: Issue fresh access token and child refresh token in the SAME session family
        LoginResponse response = createAuthResponse(user, organization, tokenEntity.getFamilyId(), clientIp, userAgent);

        // Link parent token to child token
        String newRawToken = response.getRefreshToken();
        String newTokenHash = hashToken(newRawToken);
        refreshTokenRepository.findByTokenHash(newTokenHash).ifPresent(child -> {
            tokenEntity.setReplacedByTokenId(child.getId());
            refreshTokenRepository.save(tokenEntity);
        });

        auditService.logEvent(
                organizationId,
                userId,
                "REFRESH_TOKEN_ROTATED",
                "REFRESH_TOKEN",
                tokenEntity.getId().toString(),
                null,
                "Refresh token successfully rotated"
        );

        return response;
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
    @Transactional
    public void logout(String authHeader, LogoutRequest request) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7).trim();
            jwtTokenProvider.invalidateToken(accessToken);
        }

        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            String rawToken = request.getRefreshToken().trim();
            String tokenHash = hashToken(rawToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(tokenEntity -> {
                refreshTokenRepository.revokeAllByFamilyId(tokenEntity.getFamilyId(), Instant.now(), "LOGOUT");
            });
            jwtTokenProvider.invalidateToken(rawToken);
        }

        log.info("User successfully logged out and session tokens invalidated");
    }

    @Override
    @Transactional
    public void logoutAllSessions() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        refreshTokenRepository.revokeAllByUserId(currentUserId, Instant.now(), "LOGOUT_ALL");
        log.info("All refresh sessions invalidated for user: {}", currentUserId);
    }

    private LoginResponse createAuthResponse(UserEntity user, OrganizationEntity organization) {
        return createAuthResponse(user, organization, null, null, null);
    }

    private LoginResponse createAuthResponse(UserEntity user, OrganizationEntity organization, UUID familyId, String clientIp, String userAgent) {
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

        // Generate cryptographically secure random opaque refresh token
        String rawRefreshToken = generateSecureRefreshToken();
        String tokenHash = hashToken(rawRefreshToken);

        UUID effectiveFamilyId = (familyId != null) ? familyId : UUID.randomUUID();

        RefreshTokenEntity tokenEntity = RefreshTokenEntity.builder()
                .userId(user.getId())
                .organizationId(organization != null ? organization.getId() : null)
                .tokenHash(tokenHash)
                .familyId(effectiveFamilyId)
                .expiresAt(Instant.now().plusMillis(jwtRefreshExpirationMs))
                .createdByIp(clientIp)
                .userAgent(userAgent)
                .build();

        refreshTokenRepository.save(tokenEntity);

        UserDto userDto = userMapper.toDto(user);
        OrganizationDto orgDto = organization != null ? organizationMapper.toDto(organization) : null;

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .user(userDto)
                .organization(orgDto)
                .build();
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, String clientIp) {
        String email = request.getEmail().toLowerCase().trim();
        Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCase(email);

        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            if (user.getStatus() == UserStatus.ACTIVE) {
                // 1. Invalidate any existing unused tokens for this user
                passwordResetTokenRepository.invalidateAllPendingTokensForUser(user.getId(), Instant.now());

                // 2. Generate cryptographically secure token & SHA-256 hash
                String rawToken = generateSecureToken();
                String tokenHash = hashToken(rawToken);

                // 3. Persist hashed token in database
                PasswordResetTokenEntity tokenEntity = PasswordResetTokenEntity.builder()
                        .userId(user.getId())
                        .tokenHash(tokenHash)
                        .expiresAt(Instant.now().plus(passwordResetExpirationMinutes, ChronoUnit.MINUTES))
                        .createdByIp(clientIp)
                        .build();
                passwordResetTokenRepository.save(tokenEntity);

                // 4. Construct complete reset URL with raw token
                String resetUrl = resetPasswordBaseUrl.contains("?")
                        ? resetPasswordBaseUrl + "&token=" + rawToken
                        : resetPasswordBaseUrl + "?token=" + rawToken;

                // 5. Dispatch branded password reset email
                emailNotificationService.sendPasswordResetEmail(
                        user.getEmail(),
                        user.getFullName(),
                        resetUrl,
                        passwordResetExpirationMinutes
                );

                // 6. Record audit log
                auditService.logEvent(
                        user.getOrganizationId(),
                        user.getId(),
                        "PASSWORD_RESET_REQUESTED",
                        "USER",
                        user.getId().toString(),
                        null,
                        "Password reset initiated for IP: " + (clientIp != null ? clientIp : "unknown")
                );

                log.info("Password reset token generated and email dispatched for user {}", user.getId());
            } else {
                log.warn("Password reset requested for non-active user {} (status: {})", user.getId(), user.getStatus());
            }
        } else {
            log.info("Password reset requested for non-existent email: {}", email);
        }
        // Generic return for anti-enumeration security
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request, String clientIp) {
        String rawToken = request.getToken().trim();
        String tokenHash = hashToken(rawToken);

        PasswordResetTokenEntity tokenEntity = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired password reset token"));

        if (!tokenEntity.isValid()) {
            log.warn("Attempt to use invalid/expired password reset token {} (used: {}, expired: {})",
                    tokenEntity.getId(), tokenEntity.isUsed(), tokenEntity.isExpired());
            throw new BadCredentialsException("Invalid or expired password reset token");
        }

        UserEntity user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", tokenEntity.getUserId()));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE, "User account is " + user.getStatus() + ". Password cannot be reset");
        }

        // 1. Update user password hash
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 2. Mark token as consumed
        tokenEntity.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(tokenEntity);

        // 3. Invalidate any other pending password reset tokens
        passwordResetTokenRepository.invalidateAllPendingTokensForUser(user.getId(), Instant.now());

        // 4. Invalidate all active refresh token sessions for this user upon password reset
        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now(), "PASSWORD_RESET");

        // 5. Record audit log
        auditService.logEvent(
                user.getOrganizationId(),
                user.getId(),
                "PASSWORD_RESET_COMPLETED",
                "USER",
                user.getId().toString(),
                null,
                "Password reset completed successfully for IP: " + (clientIp != null ? clientIp : "unknown")
        );

        log.info("Password successfully reset for user {}", user.getId());
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE, "User account is " + user.getStatus() + ". Password cannot be changed");
        }

        // 1. Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Failed password change attempt for user {}: current password incorrect", currentUserId);
            throw new BadCredentialsException("Current password is incorrect");
        }

        // 2. Reject same password
        if (request.getCurrentPassword().equals(request.getNewPassword()) || passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "New password must be different from your current password");
        }

        // 3. Verify confirmation match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "New password and confirm password do not match");
        }

        // 4. Update password hash
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 5. Invalidate any pending password reset tokens as safety precaution
        passwordResetTokenRepository.invalidateAllPendingTokensForUser(user.getId(), Instant.now());

        // 6. Invalidate all active refresh token sessions for this user upon password change
        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now(), "PASSWORD_CHANGED");

        // 7. Record audit log
        auditService.logEvent(
                user.getOrganizationId(),
                user.getId(),
                "PASSWORD_CHANGED",
                "USER",
                user.getId().toString(),
                null,
                "Password successfully changed by authenticated user"
        );

        log.info("Password successfully changed for user {}", user.getId());
    }

    @Scheduled(cron = "0 0 3 * * ?") // Prune expired tokens daily at 3 AM
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        // Retain revoked/expired tokens for 30 days for security investigation, then clean up
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        int deleted = refreshTokenRepository.deleteExpiredTokens(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} expired refresh tokens older than 30 days", deleted);
        }
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String generateSecureRefreshToken() {
        byte[] randomBytes = new byte[64];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
