package com.va.v.v_app.core.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard API Response wrapper
 * Provides consistent response structure across all API endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * HTTP status code
     */
    private Integer status;

    /**
     * Response message
     */
    private String message;

    /**
     * Response data (can be any type)
     */
    private T data;

    /**
     * Error details (only populated for error responses)
     */
    private String error;

    /**
     * Request path
     */
    private String path;

    /**
     * Timestamp of the response
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Create a success response
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a success response with default message
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operation completed successfully");
    }

    /**
     * Create an error response
     */
    public static <T> ApiResponse<T> error(Integer status, String error, String message, String path) {
        return ApiResponse.<T>builder()
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a bad request error response
     */
    public static <T> ApiResponse<T> badRequest(String message, String path) {
        return error(400, "Bad Request", message, path);
    }

    /**
     * Create an unauthorized error response
     */
    public static <T> ApiResponse<T> unauthorized(String message, String path) {
        return error(401, "Unauthorized", message, path);
    }

    /**
     * Create a forbidden error response
     */
    public static <T> ApiResponse<T> forbidden(String message, String path) {
        return error(403, "Forbidden", message, path);
    }

    /**
     * Create a not found error response
     */
    public static <T> ApiResponse<T> notFound(String message, String path) {
        return error(404, "Not Found", message, path);
    }

    /**
     * Create an internal server error response
     */
    public static <T> ApiResponse<T> internalServerError(String message, String path) {
        return error(500, "Internal Server Error", message, path);
    }
}
