package com.taxoryn.core.exception;

import com.taxoryn.core.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
    }

    @Test
    @DisplayName("Handle ResourceNotFoundException maps to 404 NOT_FOUND")
    void testHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "id", "123");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAppException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().getErrorCode());
        assertEquals("/api/v1/test", response.getBody().getPath());
    }

    @Test
    @DisplayName("Handle BadCredentialsException maps to 401 UNAUTHORIZED")
    void testHandleBadCredentials() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentials(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_CREDENTIALS", response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("Handle AccessDeniedException maps to 403 FORBIDDEN")
    void testHandleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FORBIDDEN", response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("Handle TenantAccessDeniedException maps to 403 TENANT_MISMATCH")
    void testHandleTenantAccessDenied() {
        TenantAccessDeniedException ex = new TenantAccessDeniedException("Cross-tenant violation");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAppException(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TENANT_MISMATCH", response.getBody().getErrorCode());
    }
}
