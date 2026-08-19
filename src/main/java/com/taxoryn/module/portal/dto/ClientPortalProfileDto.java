package com.taxoryn.module.portal.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientType;
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
@Schema(description = "Client Portal Profile Information")
public class ClientPortalProfileDto {

    private UUID clientId;
    private String displayName;
    private String legalName;
    private ClientType clientType;
    private String pan;
    private String gstin;
    private String tan;
    private String email;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private String assignedPractitionerName;
    private String assignedPractitionerEmail;
    private String assignedPractitionerPhone;
}
