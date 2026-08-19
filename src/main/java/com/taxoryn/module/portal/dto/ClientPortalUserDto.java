package com.taxoryn.module.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Client Portal User Details")
public class ClientPortalUserDto {

    private UUID userId;
    private UUID clientId;
    private String clientName;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private Set<String> roles;
}
