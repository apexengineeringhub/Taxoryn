package com.taxoryn.module.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterOrganizationResponse {

    private UUID organizationId;
    private String organizationName;
    private String adminEmail;
    private String status;
    private String message;
}
