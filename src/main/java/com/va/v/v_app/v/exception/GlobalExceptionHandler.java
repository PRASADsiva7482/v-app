package com.va.v.v_app.v.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler (B-1).
 * Catches all exceptions thrown across controllers and returns
 * consistent, structured error responses instead of stack traces.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle ResourceNotFoundException → 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND");
    }

    /**
     * Handle BusinessException → 400
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getErrorCode());
    }

    /**
     * Handle UnauthorizedException → 403
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), "FORBIDDEN");
    }

    /**
     * Handle Jakarta Bean Validation errors → 400 (B-7)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "Validation failed");
        body.put("code", "VALIDATION_ERROR");
        body.put("fieldErrors", fieldErrors);
        body.put("timestamp", LocalDateTime.now().toString());

        log.warn("Validation errors: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handle IllegalArgumentException → 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "BAD_REQUEST");
    }

    /**
     * Catch-all for unexpected exceptions → 500
     * Prevents stack traces from leaking to clients.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                "INTERNAL_ERROR");
    }

    /**
     * Build consistent error response structure
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String message, String code) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", message);
        body.put("code", code);
        body.put("status", status.value());
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
