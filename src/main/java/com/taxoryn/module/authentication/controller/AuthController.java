package com.taxoryn.module.authentication.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.authentication.dto.ActivateOrganizationRequest;
import com.taxoryn.module.authentication.dto.ChangePasswordRequest;
import com.taxoryn.module.authentication.dto.ForgotPasswordRequest;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.authentication.dto.LoginResponse;
import com.taxoryn.module.authentication.dto.LogoutRequest;
import com.taxoryn.module.authentication.dto.RefreshTokenRequest;
import com.taxoryn.module.authentication.dto.RegisterOrganizationRequest;
import com.taxoryn.module.authentication.dto.RegisterOrganizationResponse;
import com.taxoryn.module.authentication.dto.RegisterUserByAdminRequest;
import com.taxoryn.module.authentication.dto.ResendActivationRequest;
import com.taxoryn.module.authentication.dto.ResetPasswordRequest;
import com.taxoryn.module.authentication.service.AuthService;
import com.taxoryn.module.user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxoryn.core.security.AuthCookieUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import com.taxoryn.core.exception.UnauthorizedException;

@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
@RequiredArgsConstructor
@Tag(name = "Authentication & Authorization", description = "Endpoints for user authentication, token refresh, logout, tenant onboarding, and team registration")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieUtil authCookieUtil;
    private final com.taxoryn.core.security.proxy.ClientIpResolver clientIpResolver;

    @PostMapping({"/register-organization", "/register"})
    @Operation(summary = "Register organization & admin", description = "Onboards a new tenant organization in inactive state awaiting email activation.")
    public ResponseEntity<ApiResponse<RegisterOrganizationResponse>> registerOrganization(
            @Valid @RequestBody RegisterOrganizationRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        RegisterOrganizationResponse response = authService.registerOrganization(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Organization registered successfully. Please check your email to activate your account.", response));
    }

    @PostMapping({"/activate-organization", "/activate"})
    @Operation(summary = "Activate organization & admin / employee account", description = "Validates the activation token, updates password if provided, and activates the tenant organization and user.")
    public ResponseEntity<ApiResponse<Void>> activateOrganization(
            @Valid @RequestBody ActivateOrganizationRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        authService.activateOrganization(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success(
                "Account activated successfully. You can now log in.",
                null
        ));
    }

    @GetMapping({"/validate-activation-token", "/activate/verify"})
    @Operation(summary = "Validate activation token", description = "Verifies token validity and retrieves user/practice context for the onboarding activation page.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.authentication.dto.ValidateActivationTokenResponse>> validateActivationToken(
            @org.springframework.web.bind.annotation.RequestParam("token") String token) {
        com.taxoryn.module.authentication.dto.ValidateActivationTokenResponse response = authService.validateActivationToken(token);
        return ResponseEntity.ok(ApiResponse.success("Activation token is valid", response));
    }

    @PostMapping("/resend-activation")
    @Operation(summary = "Resend activation email", description = "Issues a fresh activation token and dispatches activation email to an inactive user.")
    public ResponseEntity<ApiResponse<Void>> resendActivation(
            @Valid @RequestBody ResendActivationRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        authService.resendActivation(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success(
                "If an inactive account exists for this email, an activation link has been sent.",
                null
        ));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates credentials and issues multi-tenant JWT access and refresh tokens.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        ResponseCookie cookie = authCookieUtil.createRefreshTokenCookie(response.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success("Login successful", response));
    }

    @PostMapping({"/refresh", "/refresh-token"})
    @Operation(summary = "Refresh JWT tokens", description = "Issues fresh access & refresh tokens using a valid unrevoked refresh token.")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest) {
        String rawToken = authCookieUtil.extractRefreshToken(servletRequest, request)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is required"));

        String clientIp = extractClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        LoginResponse response = authService.refreshToken(new RefreshTokenRequest(rawToken), clientIp, userAgent);

        ResponseCookie cookie = authCookieUtil.createRefreshTokenCookie(response.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success("Token refreshed successfully", response));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get current authenticated user profile", description = "Returns the profile details of the currently authenticated user based on JWT.")
    public ResponseEntity<ApiResponse<UserDto>> getMe() {
        UserDto user = authService.getMe();
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", user));
    }

    @PostMapping("/register-user")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasAuthority('USER_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Register team member (Org Admin)", description = "Allows an authenticated Organization Admin to register a new user under their own tenant organization.")
    public ResponseEntity<ApiResponse<UserDto>> registerUserByAdmin(@Valid @RequestBody RegisterUserByAdminRequest request) {
        UserDto createdUser = authService.registerUserByAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User registered successfully", createdUser));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Logout & Invalidate Tokens", description = "Revokes the active JWT access token and optional refresh token, blacklisting them from further access.")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest servletRequest) {
        String rawRefreshToken = authCookieUtil.extractRefreshToken(
                servletRequest,
                request != null && org.springframework.util.StringUtils.hasText(request.getRefreshToken()) ? new RefreshTokenRequest(request.getRefreshToken()) : null
        ).orElse(null);

        authService.logout(authHeader, new LogoutRequest(rawRefreshToken));
        ResponseCookie deleteCookie = authCookieUtil.createDeleteRefreshTokenCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(ApiResponse.success("Successfully logged out and tokens invalidated", null));
    }

    @PostMapping("/logout-all")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Logout All Sessions", description = "Revokes all active refresh token sessions for the authenticated user across all devices.")
    public ResponseEntity<ApiResponse<Void>> logoutAll() {
        authService.logoutAllSessions();
        ResponseCookie deleteCookie = authCookieUtil.createDeleteRefreshTokenCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(ApiResponse.success("All sessions successfully terminated", null));
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Change password for authenticated user", description = "Validates the current password against stored hash and sets a new password meeting policy standards.")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Your password has been changed successfully.", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate password recovery", description = "Generates a secure, time-limited reset token and sends an email with reset instructions if the account exists.")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        String origin = extractOrigin(servletRequest);
        authService.forgotPassword(request, clientIp, origin);
        return ResponseEntity.ok(ApiResponse.success(
                "If an account exists for this email, you will receive password reset instructions.",
                null
        ));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Complete password recovery", description = "Validates the raw reset token and updates the user password.")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        authService.resetPassword(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success(
                "Password has been reset successfully. You can now log in with your new password.",
                null
        ));
    }

    private String extractOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (org.springframework.util.StringUtils.hasText(origin)) {
            return origin.trim();
        }
        String referer = request.getHeader("Referer");
        if (org.springframework.util.StringUtils.hasText(referer)) {
            try {
                java.net.URI uri = java.net.URI.create(referer.trim());
                String scheme = uri.getScheme();
                String host = uri.getHost();
                int port = uri.getPort();
                if (scheme != null && host != null) {
                    if (port > 0 && port != 80 && port != 443) {
                        return scheme + "://" + host + ":" + port;
                    }
                    return scheme + "://" + host;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        return clientIpResolver.resolveClientIp(request);
    }
}
