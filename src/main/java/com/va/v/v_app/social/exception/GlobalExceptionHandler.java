package com.va.v.v_app.social.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler — HARDENED.
 *
 * Security rules enforced:
 *   ✅ NEVER expose stack traces, class names, or internal paths
 *   ✅ NEVER reveal database, table, or column names
 *   ✅ NEVER reveal server technology or framework versions
 *   ✅ All unexpected exceptions return a generic message
 *   ✅ All exceptions are logged server-side for debugging
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
        return buildErrorResponse(HttpStatus.NOT_FOUND, "The requested resource was not found.", "RESOURCE_NOT_FOUND");
    }

    /**
     * Handle BusinessException → 400
     * Only expose the business-friendly message, never internal details
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, sanitizeMessage(ex.getMessage()), ex.getErrorCode());
    }

    /**
     * Handle UnauthorizedException → 403
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.", "FORBIDDEN");
    }

    /**
     * Handle Jakarta Bean Validation errors → 400
     * Only expose field names and validation messages (safe)
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
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters.", "BAD_REQUEST");
    }

    /**
     * Handle missing request parameters → 400
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing parameter: {}", ex.getParameterName());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Required parameter is missing.", "BAD_REQUEST");
    }

    /**
     * Handle unsupported HTTP methods → 405
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMethod());
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "This HTTP method is not supported for this endpoint.", "METHOD_NOT_ALLOWED");
    }

    /**
     * Handle file upload size exceeded → 413
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("File upload size exceeded: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds the allowed limit.", "FILE_TOO_LARGE");
    }

    /**
     * Handle 404 for unknown endpoints → prevents path enumeration
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFound(NoHandlerFoundException ex) {
        log.warn("No handler found for: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "The requested endpoint does not exist.", "NOT_FOUND");
    }

    /**
     * ★ CATCH-ALL for unexpected exceptions → 500
     *
     * CRITICAL: This is the most important handler for security.
     * It ensures that NO internal details ever reach the client.
     *
     * Common things this catches and hides:
     *   - SQL errors (table/column names)
     *   - NullPointerExceptions
     *   - ClassNotFound / NoSuchMethod
     *   - File system paths
     *   - Connection strings
     *   - Stack traces
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Log the FULL exception server-side for debugging
        log.error("Unhandled exception caught by GlobalExceptionHandler", ex);

        // Return GENERIC message to client — reveal nothing
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                "INTERNAL_ERROR");
    }

    /**
     * Build consistent error response structure.
     * Never includes server name, path, class names, or stack traces.
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String message, String code) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", message);
        body.put("code", code);
        body.put("status", status.value());
        body.put("timestamp", LocalDateTime.now().toString());
        // INTENTIONALLY no "path", "trace", "exception" fields
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Sanitize exception messages to prevent info leakage.
     * Strips anything that looks like internal detail.
     */
    private String sanitizeMessage(String message) {
        if (message == null) return "An error occurred.";

        // Strip common internal info patterns
        if (message.contains("SQL") || message.contains("jdbc") ||
            message.contains("Hibernate") || message.contains("EntityManager") ||
            message.contains("NullPointer") || message.contains("ClassNotFound") ||
            message.contains("java.") || message.contains("org.springframework") ||
            message.contains("com.va.v")) {
            return "An error occurred while processing your request.";
        }

        return message;
    }
}
