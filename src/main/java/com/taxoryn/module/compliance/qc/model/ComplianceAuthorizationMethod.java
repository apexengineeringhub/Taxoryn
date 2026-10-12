package com.taxoryn.module.compliance.qc.model;

/**
 * Valid mechanisms for filing authorization by client or designated authority.
 */
public enum ComplianceAuthorizationMethod {
    CLIENT_CONSENT,              // Written sign-off or portal consent from client
    DSC_SIGN_OFF,                // Digital Signature Certificate sign-off
    PRACTITIONER_AUTHORIZATION,  // Authorized practitioner / Power of Attorney sign-off
    PORTAL_CREDENTIALS           // Statutory portal OTP / EVC verification
}
