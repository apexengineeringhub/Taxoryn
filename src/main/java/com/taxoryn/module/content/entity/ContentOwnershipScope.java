package com.taxoryn.module.content.entity;

/**
 * Multi-tenancy ownership scope for Taxoryn Learn content.
 * Platform content is globally accessible knowledge, whereas Practice content is tenant-scoped.
 */
public enum ContentOwnershipScope {
    PLATFORM,
    PRACTICE
}
