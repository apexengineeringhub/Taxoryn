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

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAuthority('USER_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List users with pagination", description = "Retrieves paginated list of users for the authenticated tenant organization.")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getUsers(@Valid @ModelAttribute PageRequestDto pageRequest) {
        PagedResponse<UserDto> response = userService.getUsers(pageRequest);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", response));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAuthority('USER_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get user by ID", description = "Retrieves specific user details within the authenticated tenant.")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable UUID userId) {
        UserDto dto = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", dto));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE') or hasAuthority('USER_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create user", description = "Creates a new team member within the authenticated tenant organization.")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User created successfully", created));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_UPDATE') or hasAuthority('USER_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update user", description = "Updates details of a team member within the authenticated tenant.")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest request) {
        UserDto updated = userService.updateUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updated));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_DELETE') or hasAuthority('USER_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Deactivate user", description = "Deactivates a user within the authenticated tenant.")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", null));
    }
}
