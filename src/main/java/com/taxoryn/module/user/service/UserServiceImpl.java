package com.taxoryn.module.user.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.service.RoleService;
import com.taxoryn.module.user.dto.CreateUserRequest;
import com.taxoryn.module.user.dto.UpdateUserRequest;
import com.taxoryn.module.user.dto.UserDto;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.mapper.UserMapper;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserDto> getUsers(PageRequestDto pageRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Page<UserEntity> page = userRepository.findAllByOrganizationId(organizationId, pageRequest.toPageable());
        return PagedResponse.of(page, userMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(UUID userId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity entity = getUserEntityById(userId, organizationId);
        return userMapper.toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUserProfile() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity entity = getUserEntityById(userId, organizationId);
        return userMapper.toDto(entity);
    }

    @Override
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        if (userRepository.existsByOrganizationIdAndEmailIgnoreCase(organizationId, request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // 1. RBAC Privilege Escalation & Delegation Boundary Check
        SecurityUtils.validateRoleDelegation(request.getRoleCodes(), null);

        List<RoleEntity> roles = roleService.getRolesByCodes(request.getRoleCodes(), organizationId);

        UserEntity user = UserEntity.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName() != null ? request.getLastName().trim() : null)
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(roles))
                .build();
        user.setOrganizationId(organizationId);

        UserEntity saved = userRepository.save(user);
        log.info("Created user {} in organization {}", saved.getId(), organizationId);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public UserDto updateUser(UUID userId, UpdateUserRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = getUserEntityById(userId, organizationId);

        user.setFirstName(request.getFirstName().trim());
        if (request.getLastName() != null) user.setLastName(request.getLastName().trim());
        if (request.getPhone() != null) user.setPhone(request.getPhone());

        boolean isCurrentlyOrgAdmin = user.getRoles().stream().anyMatch(r -> "ORG_ADMIN".equals(r.getCode()));

        // Prevent deactivating the last remaining active ORG_ADMIN
        if (request.getStatus() != null) {
            if (isCurrentlyOrgAdmin && request.getStatus() != UserStatus.ACTIVE && user.getStatus() == UserStatus.ACTIVE) {
                long activeAdminCount = userRepository.countActiveOrgAdmins(organizationId);
                if (activeAdminCount <= 1) {
                    throw new com.taxoryn.core.exception.BusinessValidationException(
                            "Cannot deactivate the sole remaining active Organization Administrator"
                    );
                }
            }
            user.setStatus(request.getStatus());
        }

        if (request.getRoleCodes() != null && !request.getRoleCodes().isEmpty()) {
            // 1. RBAC Privilege Escalation & Delegation Boundary Check
            SecurityUtils.validateRoleDelegation(request.getRoleCodes(), userId);

            List<RoleEntity> roles = roleService.getRolesByCodes(request.getRoleCodes(), organizationId);

            // 2. Prevent removing ORG_ADMIN from sole remaining admin
            boolean willBeOrgAdmin = roles.stream().anyMatch(r -> "ORG_ADMIN".equals(r.getCode()));
            if (isCurrentlyOrgAdmin && !willBeOrgAdmin) {
                long activeAdminCount = userRepository.countActiveOrgAdmins(organizationId);
                if (activeAdminCount <= 1) {
                    throw new com.taxoryn.core.exception.BusinessValidationException(
                            "Cannot demote the sole remaining active Organization Administrator"
                    );
                }
            }

            user.setRoles(new HashSet<>(roles));
        }

        UserEntity saved = userRepository.save(user);
        log.info("Updated user {} in organization {}", saved.getId(), organizationId);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = getUserEntityById(userId, organizationId);

        // Prevent deactivating the last remaining active ORG_ADMIN
        boolean isCurrentlyOrgAdmin = user.getRoles().stream().anyMatch(r -> "ORG_ADMIN".equals(r.getCode()));
        if (isCurrentlyOrgAdmin && user.getStatus() == UserStatus.ACTIVE) {
            long activeAdminCount = userRepository.countActiveOrgAdmins(organizationId);
            if (activeAdminCount <= 1) {
                throw new com.taxoryn.core.exception.BusinessValidationException(
                        "Cannot deactivate the sole remaining active Organization Administrator"
                );
            }
        }

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        log.info("Deactivated user {} in organization {}", userId, organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserEntity getUserEntityById(UUID userId, UUID organizationId) {
        return userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
