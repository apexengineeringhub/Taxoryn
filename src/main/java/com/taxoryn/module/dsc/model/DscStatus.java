package com.taxoryn.module.dsc.model;

/**
 * Lifecycle status of a Digital Signature Certificate (DSC).
 */
public enum DscStatus {
    ACTIVE,
    EXPIRING,
    EXPIRED,
    REVOKED,
    INACTIVE
}
