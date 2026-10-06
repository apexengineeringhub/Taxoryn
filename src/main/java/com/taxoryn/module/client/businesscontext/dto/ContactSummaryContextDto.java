package com.taxoryn.module.client.businesscontext.dto;

import com.taxoryn.module.client.entity.ContactRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Primary contact summary in business context")
public class ContactSummaryContextDto {

    @Schema(description = "Contact ID")
    private UUID id;

    @Schema(description = "Full name of the contact")
    private String name;

    @Schema(description = "Contact role")
    private ContactRole role;

    @Schema(description = "Email address")
    private String email;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "Designation / title")
    private String designation;
}
