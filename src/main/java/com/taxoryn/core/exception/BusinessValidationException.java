package com.taxoryn.core.exception;

public class BusinessValidationException extends AppException {

    public BusinessValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
    }
}
