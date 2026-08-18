package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Client Details Payload")
public class ClientDto {

    private UUID id;
    private UUID organizationId;
    private ClientType clientType;
    private String displayName;
    private String legalName;
    private String pan;
    private String gstin;
    private String email;
    private String phone;
    private ClientStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
