package com.taxoryn.module.notification.whatsapp.event;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class UserRegisteredEvent {
    UUID userId;
    UUID organizationId;
    UserRegistrationType registrationType;
    String firstName;
    String lastName;
    String organizationName;
    String email;
    String phone;
    @Builder.Default
    Instant registeredAt = Instant.now();

    public String getFullName() {
        if (firstName == null && lastName == null) return "Valued User";
        if (lastName == null || lastName.isBlank()) return firstName;
        if (firstName == null || firstName.isBlank()) return lastName;
        return firstName.trim() + " " + lastName.trim();
    }
}
