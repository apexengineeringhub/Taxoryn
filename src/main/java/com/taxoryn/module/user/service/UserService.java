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

    UserDto createUser(CreateUserRequest request);

    UserDto updateUser(UUID userId, UpdateUserRequest request);

    void deleteUser(UUID userId);

    UserEntity getUserEntityById(UUID userId, UUID organizationId);
}
