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

@Entity
@Table(name = "clients")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientEntity extends TenantAuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 50)
    @Builder.Default
    private ClientType clientType = ClientType.INDIVIDUAL;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "pan", length = 10)
    private String pan;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "email")
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ClientStatus status = ClientStatus.ACTIVE;

    public enum ClientType {
        INDIVIDUAL,
        COMPANY,
        PARTNERSHIP,
        LLP,
        HUF,
        TRUST,
        SOCIETY,
        AOP_BOI
    }

    public enum ClientStatus {
        ACTIVE,
        INACTIVE,
        PROSPECT,
        ARCHIVED
    }
}
