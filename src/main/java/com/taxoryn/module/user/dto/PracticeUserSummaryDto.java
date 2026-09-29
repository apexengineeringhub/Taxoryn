package com.taxoryn.module.user.dto;

import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeUserSummaryDto {
    private UUID organizationId;
    private String organizationName;
    private String legalName;
    private String tradeName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String country;
    private OrganizationEntity.OrganizationStatus status;
    private OrganizationType organizationType;
    private OrganizationEntity.SubscriptionPlan subscriptionPlan;
    
    @Builder.Default
    private List<AdminUserSummaryDto> admins = new ArrayList<>();
    
    private long adminCount;
    private long totalUserCount;
    private long activeUserCount;
    private Instant createdAt;
}
