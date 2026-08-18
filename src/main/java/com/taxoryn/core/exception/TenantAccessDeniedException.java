package com.taxoryn.core.exception;

public class TenantAccessDeniedException extends AppException {

    public TenantAccessDeniedException(String message) {
        super(ErrorCode.TENANT_MISMATCH, message);
    }

    public TenantAccessDeniedException() {
        super(ErrorCode.TENANT_MISMATCH, "Cross-tenant access violation: Action denied for this organization");
    }
}
