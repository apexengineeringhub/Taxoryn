package com.taxoryn.module.user.dto;

import com.taxoryn.module.role.dto.RoleDto;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User Profile Payload")
public class UserDto {

    private UUID id;
    private UUID organizationId;
    private UUID clientId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private UserStatus status;
    private Set<RoleDto> roles;
    private Instant createdAt;
    private Instant updatedAt;
}
