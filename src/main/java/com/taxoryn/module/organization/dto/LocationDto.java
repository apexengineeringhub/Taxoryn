package com.taxoryn.module.organization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {

    private UUID id;
    private UUID organizationId;
    private String name;
    private String code;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private String phone;
    private String email;

    @JsonProperty("isHeadOffice")
    private boolean isHeadOffice;

    @JsonProperty("isActive")
    private boolean isActive;

    private List<UUID> assignedEmployeeIds;
    private int assignedEmployeeCount;
    private Instant createdAt;
    private Instant updatedAt;
}
