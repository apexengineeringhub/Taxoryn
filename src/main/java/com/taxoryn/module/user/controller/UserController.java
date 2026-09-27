package com.taxoryn.module.user.controller;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.user.dto.CreateUserRequest;
import com.taxoryn.module.user.dto.UpdateUserRequest;
import com.taxoryn.module.user.dto.UserDto;
import com.taxoryn.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing team members within the tenant organization")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current authenticated user profile", description = "Retrieves self-service profile details for the authenticated user.")
    public ResponseEntity<ApiResponse<UserDto>> getMyProfile() {
        UserDto dto = userService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", dto));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update self-service profile", description = "Allows any authenticated user to update their permitted profile fields (first name, last name, phone, avatar).")
    public ResponseEntity<ApiResponse<UserDto>> updateMyProfile(@Valid @RequestBody com.taxoryn.module.user.dto.UpdateUserProfileRequest request) {
        UserDto updated = userService.updateMyProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @PostMapping(value = "/me/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload user profile avatar", description = "Uploads and scans an avatar photo for the authenticated user.")
    public ResponseEntity<ApiResponse<UserDto>> uploadMyAvatar(
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        UserDto updated = userService.uploadMyAvatar(file);
        return ResponseEntity.ok(ApiResponse.success("Avatar uploaded successfully", updated));
    }

    @DeleteMapping("/me/avatar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete user profile avatar", description = "Removes the avatar photo for the authenticated user.")
    public ResponseEntity<ApiResponse<Void>> deleteMyAvatar() {
        userService.deleteMyAvatar();
        return ResponseEntity.ok(ApiResponse.success("Avatar deleted successfully", null));
    }

    @GetMapping("/me/avatar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Stream own avatar image", description = "Streams the avatar binary image for the authenticated user.")
    public ResponseEntity<byte[]> streamMyAvatar() {
        com.taxoryn.module.user.service.ProfileImageService.AvatarContent avatar = userService.getMyAvatar();
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(avatar.getContentType()))
                .contentLength(avatar.getFileSize())
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "private, no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .header("X-Content-Type-Options", "nosniff")
                .body(avatar.getData());
    }

    @GetMapping("/{userId}/avatar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Stream user avatar image", description = "Streams the avatar binary image for a specific user in the tenant.")
    public ResponseEntity<byte[]> streamUserAvatar(@PathVariable UUID userId) {
        com.taxoryn.module.user.service.ProfileImageService.AvatarContent avatar = userService.getAvatar(userId);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(avatar.getContentType()))
                .contentLength(avatar.getFileSize())
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "private, no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .header("X-Content-Type-Options", "nosniff")
                .body(avatar.getData());
    }

    @GetMapping("/me/context")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get aggregated current user context", description = "Retrieves current user details, tenant organization, practice profile, role, location access scope, and effective module configuration.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.user.dto.UserContextDto>> getCurrentUserContext() {
        com.taxoryn.module.user.dto.UserContextDto context = userService.getCurrentUserContext();
        return ResponseEntity.ok(ApiResponse.success("User context retrieved successfully", context));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAuthority('USER_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER')")
    @Operation(summary = "List users with pagination", description = "Retrieves paginated list of users for the authenticated tenant organization.")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getUsers(@Valid @ModelAttribute PageRequestDto pageRequest) {
        PagedResponse<UserDto> response = userService.getUsers(pageRequest);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", response));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAuthority('USER_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER')")
    @Operation(summary = "Get user by ID", description = "Retrieves specific user details within the authenticated tenant.")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable UUID userId) {
        UserDto dto = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", dto));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE') or hasAuthority('USER_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER')")
    @Operation(summary = "Create user", description = "Creates a new team member within the authenticated tenant organization.")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User created successfully", created));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_UPDATE') or hasAuthority('USER_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER')")
    @Operation(summary = "Update user", description = "Updates details of a team member within the authenticated tenant.")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest request) {
        UserDto updated = userService.updateUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updated));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_DELETE') or hasAuthority('USER_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER')")
    @Operation(summary = "Deactivate user", description = "Deactivates a user within the authenticated tenant.")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", null));
    }

    @GetMapping("/{userId}/locations")
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAuthority('USER_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER')")
    @Operation(summary = "Get user's accessible locations", description = "Retrieves all locations assigned to or accessible by a user in the practice.")
    public ResponseEntity<ApiResponse<java.util.List<com.taxoryn.module.organization.dto.LocationDto>>> getUserLocations(@PathVariable UUID userId) {
        java.util.List<com.taxoryn.module.organization.dto.LocationDto> locations = userService.getUserLocations(userId);
        return ResponseEntity.ok(ApiResponse.success("User locations retrieved successfully", locations));
    }

    @PutMapping("/{userId}/locations")
    @PreAuthorize("hasAuthority('USER_UPDATE') or hasAuthority('USER_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER')")
    @Operation(summary = "Assign locations to user", description = "Updates the location access roster assigned to a user.")
    public ResponseEntity<ApiResponse<java.util.List<com.taxoryn.module.organization.dto.LocationDto>>> assignUserLocations(
            @PathVariable UUID userId,
            @Valid @RequestBody com.taxoryn.module.user.dto.AssignUserLocationsRequest request) {
        java.util.List<com.taxoryn.module.organization.dto.LocationDto> locations = userService.assignUserLocations(userId, request.getLocationIds());
        return ResponseEntity.ok(ApiResponse.success("User locations assigned successfully", locations));
    }

    @PostMapping("/{userId}/locations/{locationId}")
    @PreAuthorize("hasAuthority('USER_UPDATE') or hasAuthority('USER_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER')")
    @Operation(summary = "Assign single location to user", description = "Adds a single location to the user's assigned location scope.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.organization.dto.LocationDto>> assignUserLocation(
            @PathVariable UUID userId,
            @PathVariable UUID locationId) {
        com.taxoryn.module.organization.dto.LocationDto location = userService.assignUserLocation(userId, locationId);
        return ResponseEntity.ok(ApiResponse.success("Location assigned to user successfully", location));
    }

    @DeleteMapping("/{userId}/locations/{locationId}")
    @PreAuthorize("hasAuthority('USER_UPDATE') or hasAuthority('USER_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER')")
    @Operation(summary = "Remove location from user", description = "Removes a location from the user's assigned location scope.")
    public ResponseEntity<ApiResponse<Void>> removeUserLocation(
            @PathVariable UUID userId,
            @PathVariable UUID locationId) {
        userService.removeUserLocation(userId, locationId);
        return ResponseEntity.ok(ApiResponse.success("Location removed from user successfully", null));
    }

    @PutMapping("/{userId}/status")
    @PreAuthorize("hasAuthority('USER_UPDATE') or hasAuthority('USER_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER')")
    @Operation(summary = "Update user lifecycle status", description = "Activates or deactivates a user account within the tenant.")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody com.taxoryn.module.user.dto.UpdateUserStatusRequest request) {
        UserDto updated = userService.updateUserStatus(userId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", updated));
    }
}
