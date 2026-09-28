package com.taxoryn.module.user.dto;

import com.taxoryn.module.user.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSummaryDto {
    private UUID id;
    private String fullName;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String roleCode;
    private String roleDisplayName;
    private UserEntity.UserStatus status;
    private String avatarUrl;
}
