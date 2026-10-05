package com.taxoryn.module.client.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "client_branches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientBranchEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "branch_name", nullable = false, length = 150)
    private String branchName;

    @Column(name = "branch_code", length = 50)
    private String branchCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "branch_type", nullable = false, length = 50)
    @Builder.Default
    private ClientBranchType branchType = ClientBranchType.BRANCH;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "state_code", length = 10)
    private String stateCode;

    @Column(name = "country", length = 100)
    @Builder.Default
    private String country = "India";

    @Column(name = "pincode", length = 20)
    private String pincode;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "primary_branch", nullable = false)
    @Builder.Default
    private boolean primaryBranch = false;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
