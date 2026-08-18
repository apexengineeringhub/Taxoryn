package com.taxoryn.module.authentication.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.authentication.dto.LoginResponse;
import com.taxoryn.module.authentication.dto.LogoutRequest;
import com.taxoryn.module.authentication.dto.RefreshTokenRequest;
import com.taxoryn.module.authentication.dto.RegisterOrganizationRequest;
import com.taxoryn.module.authentication.dto.RegisterUserByAdminRequest;
import com.taxoryn.module.authentication.service.AuthService;
import com.taxoryn.module.user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
@RequiredArgsConstructor
@Tag(name = "Authentication & Authorization", description = "Endpoints for user authentication, token refresh, logout, tenant onboarding, and team registration")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register-organization")
    @Operation(summary = "Register organization & admin", description = "Onboards a new tenant organization and creates its initial primary administrator account.")
    public ResponseEntity<ApiResponse<LoginResponse>> registerOrganization(@Valid @RequestBody RegisterOrganizationRequest request) {
        LoginResponse response = authService.registerOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Organization and administrator registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates credentials and issues multi-tenant JWT access and refresh tokens.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping({"/refresh", "/refresh-token"})
    @Operation(summary = "Refresh JWT tokens", description = "Issues fresh access & refresh tokens using a valid unrevoked refresh token.")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
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
            @RequestBody(required = false) LogoutRequest request) {
        authService.logout(authHeader, request);
        return ResponseEntity.ok(ApiResponse.success("Successfully logged out and tokens invalidated", null));
    }
}
