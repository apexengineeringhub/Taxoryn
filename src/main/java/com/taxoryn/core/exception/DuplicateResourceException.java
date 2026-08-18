package com.taxoryn.core.exception;

public class DuplicateResourceException extends AppException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.RESOURCE_ALREADY_EXISTS, message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(ErrorCode.RESOURCE_ALREADY_EXISTS, String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
