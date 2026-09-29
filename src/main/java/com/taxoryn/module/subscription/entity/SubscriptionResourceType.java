package com.taxoryn.module.subscription.entity;

public enum SubscriptionResourceType {
    TEAM_MEMBER("Team Members", "Internal practice staff and practitioner accounts"),
    CLIENT("Active Clients", "Active client entities managed in the system"),
    STORAGE("Vault Storage", "Document vault storage consumed"),
    LOCATION("Locations", "Physical practice branch offices");

    private final String displayName;
    private final String description;

    SubscriptionResourceType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
