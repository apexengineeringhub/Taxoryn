package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ContactRole;
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
@Schema(description = "Client Contact Details")
public class ClientContactDto {

    @Schema(description = "Contact ID")
    private UUID id;

    @Schema(description = "Owning organization ID")
    private UUID organizationId;

    @Schema(description = "Associated client ID")
    private UUID clientId;

    @Schema(description = "First name")
    private String firstName;

    @Schema(description = "Last name")
    private String lastName;

    @Schema(description = "Computed or explicit display name")
    private String displayName;

    @Schema(description = "Designation / job title")
    private String designation;

    @Schema(description = "Email address")
    private String email;

    @Schema(description = "Primary phone number")
    private String phone;

    @Schema(description = "Alternate phone number")
    private String altPhone;

    @Schema(description = "Contact role / function")
    private ContactRole contactRole;

    @Schema(description = "Whether this is the designated primary contact for the client")
    private boolean primaryContact;

    @Schema(description = "Active status")
    private boolean active;

    @Schema(description = "Additional notes")
    private String notes;

    @Schema(description = "Created at timestamp")
    private Instant createdAt;

    @Schema(description = "Updated at timestamp")
    private Instant updatedAt;
}
