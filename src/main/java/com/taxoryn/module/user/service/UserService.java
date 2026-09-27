package com.taxoryn.module.user.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.user.dto.CreateUserRequest;
import com.taxoryn.module.user.dto.UpdateUserRequest;
import com.taxoryn.module.user.dto.UserDto;
import com.taxoryn.module.user.entity.UserEntity;

import java.util.UUID;

public interface UserService {

    PagedResponse<UserDto> getUsers(PageRequestDto pageRequest);

    UserDto getUserById(UUID userId);

    UserDto getCurrentUserProfile();

    UserDto updateMyProfile(com.taxoryn.module.user.dto.UpdateUserProfileRequest request);

    UserDto uploadMyAvatar(org.springframework.web.multipart.MultipartFile file);

    void deleteMyAvatar();

    ProfileImageService.AvatarContent getAvatar(UUID userId);

    ProfileImageService.AvatarContent getMyAvatar();

    byte[] getAvatarContent(UUID userId);

    byte[] getMyAvatarContent();

    UserDto createUser(CreateUserRequest request);

    UserDto updateUser(UUID userId, UpdateUserRequest request);

    void deleteUser(UUID userId);

    UserEntity getUserEntityById(UUID userId, UUID organizationId);

    com.taxoryn.module.user.dto.UserContextDto getCurrentUserContext();

    java.util.List<com.taxoryn.module.organization.dto.LocationDto> getUserLocations(UUID userId);

    java.util.List<com.taxoryn.module.organization.dto.LocationDto> assignUserLocations(UUID userId, java.util.List<UUID> locationIds);

    com.taxoryn.module.organization.dto.LocationDto assignUserLocation(UUID userId, UUID locationId);

    void removeUserLocation(UUID userId, UUID locationId);

    UserDto updateUserStatus(UUID userId, com.taxoryn.module.user.entity.UserEntity.UserStatus status);

    UserDto updateUserRole(UUID userId, java.util.Set<String> roleCodes);
}
