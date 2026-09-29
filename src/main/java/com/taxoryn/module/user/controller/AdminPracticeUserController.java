package com.taxoryn.module.user.controller;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.user.dto.AdminUserSummaryDto;
import com.taxoryn.module.user.dto.PracticeUserSummaryDto;
import com.taxoryn.module.user.dto.UserDto;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.mapper.UserMapper;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.organization.specification.PracticeSpecification;
import com.taxoryn.module.user.specification.PracticeUserSpecification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping({"/api/v1/admin/practices", "/api/admin/practices"})
@RequiredArgsConstructor
@Tag(name = "Platform Practice-Centric User Governance", description = "Endpoints for platform SuperAdmin and Operations Admins to manage practice tenants, practice administrators, and practice-scoped users")
@SecurityRequirement(name = "BearerAuth")
public class AdminPracticeUserController {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @GetMapping
    @PreAuthorize("hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasRole('TAXORYN_SUPPORT_ADMIN') or hasRole('TAXORYN_SECURITY_ADMIN') or hasAuthority('PLATFORM_USER_VIEW')")
    @Transactional(readOnly = true)
    @Operation(summary = "List practice summaries with user and admin metrics", description = "Retrieves paginated practice-level summaries with aggregated user counts and administrator details via optimized batch queries (Zero N+1).")
    public ResponseEntity<ApiResponse<PagedResponse<PracticeUserSummaryDto>>> getPracticeSummaries(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        OrganizationEntity.OrganizationStatus parsedStatus = null;
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            try {
                parsedStatus = OrganizationEntity.OrganizationStatus.valueOf(status.toUpperCase().trim());
            } catch (IllegalArgumentException ignored) {
            }
        }

        String cleanSearch = StringUtils.hasText(search) ? search.trim() : null;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.ASC, "name"));
        Page<OrganizationEntity> orgsPage = organizationRepository.findAll(PracticeSpecification.withFilters(cleanSearch, parsedStatus), pageable);

        if (orgsPage.isEmpty()) {
            PagedResponse<PracticeUserSummaryDto> emptyResponse = PagedResponse.<PracticeUserSummaryDto>builder()
                    .content(Collections.emptyList())
                    .pageNumber(orgsPage.getNumber())
                    .pageSize(orgsPage.getSize())
                    .totalElements(orgsPage.getTotalElements())
                    .totalPages(orgsPage.getTotalPages())
                    .isFirst(orgsPage.isFirst())
                    .isLast(orgsPage.isLast())
                    .hasNext(orgsPage.hasNext())
                    .hasPrevious(orgsPage.hasPrevious())
                    .build();
            return ResponseEntity.ok(ApiResponse.success("Practice summaries retrieved successfully", emptyResponse));
        }

        List<UUID> orgIds = orgsPage.getContent().stream()
                .map(OrganizationEntity::getId)
                .collect(Collectors.toList());

        // Batch Query 1: Aggregate total and active user counts in a single GROUP BY query
        List<Object[]> countRows = userRepository.findUserCountsByOrganizationIds(orgIds);
        Map<UUID, long[]> countsMap = new HashMap<>();
        for (Object[] row : countRows) {
            UUID orgId = (UUID) row[0];
            long total = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            long active = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            countsMap.put(orgId, new long[]{total, active});
        }

        // Batch Query 2: Fetch all practice administrators in a single query with JOIN FETCH
        List<UserEntity> adminUsers = userRepository.findAdminsByOrganizationIds(orgIds);
        Map<UUID, List<AdminUserSummaryDto>> adminsByOrg = adminUsers.stream()
                .collect(Collectors.groupingBy(
                        UserEntity::getOrganizationId,
                        Collectors.mapping(this::mapToAdminSummary, Collectors.toList())
                ));

        // Assemble DTOs for the page
        List<PracticeUserSummaryDto> dtos = orgsPage.getContent().stream()
                .map(org -> {
                    long[] counts = countsMap.getOrDefault(org.getId(), new long[]{0L, 0L});
                    List<AdminUserSummaryDto> admins = adminsByOrg.getOrDefault(org.getId(), Collections.emptyList());
                    return PracticeUserSummaryDto.builder()
                            .organizationId(org.getId())
                            .organizationName(org.getName())
                            .legalName(org.getLegalName())
                            .tradeName(org.getTradeName())
                            .email(org.getEmail())
                            .phone(org.getPhone())
                            .address(org.getAddress())
                            .city(org.getCity())
                            .state(org.getState())
                            .country(org.getCountry())
                            .status(org.getStatus())
                            .organizationType(org.getOrganizationType())
                            .subscriptionPlan(org.getSubscriptionPlan())
                            .admins(admins)
                            .adminCount(admins.size())
                            .totalUserCount(counts[0])
                            .activeUserCount(counts[1])
                            .createdAt(org.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        PagedResponse<PracticeUserSummaryDto> response = PagedResponse.<PracticeUserSummaryDto>builder()
                .content(dtos)
                .pageNumber(orgsPage.getNumber())
                .pageSize(orgsPage.getSize())
                .totalElements(orgsPage.getTotalElements())
                .totalPages(orgsPage.getTotalPages())
                .isFirst(orgsPage.isFirst())
                .isLast(orgsPage.isLast())
                .hasNext(orgsPage.hasNext())
                .hasPrevious(orgsPage.hasPrevious())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Practice summaries retrieved successfully", response));
    }

    @GetMapping("/{organizationId}/users")
    @PreAuthorize("hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasRole('TAXORYN_SUPPORT_ADMIN') or hasRole('TAXORYN_SECURITY_ADMIN') or hasAuthority('PLATFORM_USER_VIEW')")
    @Transactional(readOnly = true)
    @Operation(summary = "List practice-scoped users with pagination", description = "Retrieves paginated list of users within a specific practice organization.")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getPracticeUsers(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization", "id", organizationId);
        }

        UserEntity.UserStatus parsedStatus = null;
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            try {
                parsedStatus = UserEntity.UserStatus.valueOf(status.toUpperCase().trim());
            } catch (IllegalArgumentException ignored) {
            }
        }

        String cleanRole = StringUtils.hasText(role) && !"ALL".equalsIgnoreCase(role) ? role.trim() : null;
        String cleanSearch = StringUtils.hasText(search) ? search.trim() : null;

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserEntity> usersPage = userRepository.findAll(PracticeUserSpecification.withFilters(organizationId, cleanSearch, cleanRole, parsedStatus), pageable);

        List<UserDto> dtos = userMapper.toDtoList(usersPage.getContent());
        PagedResponse<UserDto> response = PagedResponse.<UserDto>builder()
                .content(dtos)
                .pageNumber(usersPage.getNumber())
                .pageSize(usersPage.getSize())
                .totalElements(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .isFirst(usersPage.isFirst())
                .isLast(usersPage.isLast())
                .hasNext(usersPage.hasNext())
                .hasPrevious(usersPage.hasPrevious())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Practice users retrieved successfully", response));
    }

    private AdminUserSummaryDto mapToAdminSummary(UserEntity u) {
        String roleCode = "PRACTICE_ADMIN";
        String roleDisplayName = "Practice Admin";
        if (u.getRoles() != null && !u.getRoles().isEmpty()) {
            RoleEntity primaryRole = u.getRoles().stream()
                    .filter(r -> "SUPER_ADMIN".equals(r.getCode())
                            || "TAXORYN_SUPERADMIN".equals(r.getCode())
                            || "PRACTICE_OWNER".equals(r.getCode())
                            || "PRACTICE_ADMIN".equals(r.getCode())
                            || "ORG_ADMIN".equals(r.getCode()))
                    .findFirst()
                    .orElse(u.getRoles().iterator().next());
            roleCode = primaryRole.getCode();
            roleDisplayName = primaryRole.getName() != null && !primaryRole.getName().isBlank()
                    ? primaryRole.getName()
                    : primaryRole.getCode().replace('_', ' ');
        }
        return AdminUserSummaryDto.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .roleCode(roleCode)
                .roleDisplayName(roleDisplayName)
                .status(u.getStatus())
                .avatarUrl(u.getAvatarUrl())
                .build();
    }
}
