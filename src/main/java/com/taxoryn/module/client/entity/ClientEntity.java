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

import java.time.LocalDate;
import java.util.UUID;

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

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "pan", length = 10)
    private String pan;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "tan", length = 10)
    private String tan;

    @Column(name = "cin", length = 21)
    private String cin;

    @Column(name = "date_of_incorporation")
    private LocalDate dateOfIncorporation;

    @Column(name = "email")
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "alt_phone", length = 20)
    private String altPhone;

    @Column(name = "contact_person_name", length = 100)
    private String contactPersonName;

    @Column(name = "contact_person_designation", length = 100)
    private String contactPersonDesignation;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    @Builder.Default
    private String country = "India";

    @Column(name = "pincode", length = 20)
    private String pincode;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ClientStatus status = ClientStatus.ACTIVE;

    public enum ClientType {
        INDIVIDUAL,
        PROPRIETORSHIP,
        PARTNERSHIP,
        LLP,
        PRIVATE_LIMITED,
        PUBLIC_LIMITED,
        TRUST,
        SOCIETY,
        OTHER
    }

    public enum ClientStatus {
        ACTIVE,
        INACTIVE,
        PROSPECT,
        ARCHIVED
    }
}
