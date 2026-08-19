package com.taxoryn.core.exception;

public class InternalServerException extends AppException {

    public InternalServerException(String message) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, message);
    }

    public InternalServerException(String message, Throwable cause) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, message, cause);
    }
}
