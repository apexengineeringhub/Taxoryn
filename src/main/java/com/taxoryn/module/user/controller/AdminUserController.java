package com.taxoryn.module.user.controller;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.user.dto.UserDto;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.mapper.UserMapper;
import com.taxoryn.module.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/v1/admin/users", "/api/admin/users"})
@RequiredArgsConstructor
@Tag(name = "Platform Admin User Governance", description = "Endpoints for platform SuperAdmin to monitor, filter, and govern users across all organizations")
@SecurityRequirement(name = "BearerAuth")
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional(readOnly = true)
    @Operation(summary = "List all platform users", description = "Retrieves paginated list of all users across the platform with filtering by role, status, and search query.")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getPlatformUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserEntity> pageResult = userRepository.findAll(pageable);

        List<UserEntity> filtered = pageResult.getContent().stream()
                .filter(u -> {
                    if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
                        boolean hasRole = u.getRoles() != null && u.getRoles().stream()
                                .anyMatch(r -> r.getCode().equalsIgnoreCase(role));
                        if (!hasRole) return false;
                    }
                    if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                        if (u.getStatus() == null || !u.getStatus().name().equalsIgnoreCase(status)) {
                            return false;
                        }
                    }
                    if (search != null && !search.isBlank()) {
                        String q = search.toLowerCase();
                        boolean matchesEmail = u.getEmail() != null && u.getEmail().toLowerCase().contains(q);
                        boolean matchesName = (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(q))
                                || (u.getLastName() != null && u.getLastName().toLowerCase().contains(q));
                        if (!matchesEmail && !matchesName) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        List<UserDto> dtos = userMapper.toDtoList(filtered);
        PagedResponse<UserDto> response = PagedResponse.<UserDto>builder()
                .content(dtos)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isFirst(pageResult.isFirst())
                .isLast(pageResult.isLast())
                .hasNext(pageResult.hasNext())
                .hasPrevious(pageResult.hasPrevious())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Platform users retrieved successfully", response));
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    @Operation(summary = "Update user status", description = "Allows SuperAdmin to activate, suspend, or deactivate any platform user.")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam UserEntity.UserStatus status
    ) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setStatus(status);
        UserEntity saved = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("User status updated to " + status, userMapper.toDto(saved)));
    }
}
