package com.taxoryn.core.exception;

public class BadRequestException extends AppException {

    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(ErrorCode.BAD_REQUEST, message, cause);
    }
}
