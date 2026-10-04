package com.taxoryn.module.gov.model;

/**
 * Types of credentials supported for government integrations.
 */
public enum GovCredentialType {
    /**
     * API key / client secret based authentication.
     */
    API_KEY,

    /**
     * Username and password credentials.
     */
    BASIC_AUTH,

    /**
     * OAuth2 access / refresh token pair.
     */
    OAUTH2_TOKEN,

    /**
     * Digital Signature Certificate / Token credential reference.
     */
    CERTIFICATE_DSC,

    /**
     * Provider-specific custom or hybrid auth token.
     */
    CUSTOM
}
