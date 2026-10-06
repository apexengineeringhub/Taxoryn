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
@Table(name = "client_contacts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientContactEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "email")
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "alt_phone", length = 20)
    private String altPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_role", nullable = false, length = 50)
    @Builder.Default
    private ContactRole contactRole = ContactRole.OTHER;

    @Column(name = "primary_contact", nullable = false)
    @Builder.Default
    private boolean primaryContact = false;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public String getComputedDisplayName() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        if (lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        }
        return firstName != null ? firstName : "";
    }

    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    public void ensureDisplayName() {
        if (this.displayName == null || this.displayName.isBlank()) {
            this.displayName = getComputedDisplayName();
        }
    }
}
