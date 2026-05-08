package com.va.v.v_app.social.exception;

/**
 * Custom exception for unauthorized access / permission denied (403).
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String resource, String action) {
        super(String.format("You don't have permission to %s this %s", action, resource));
    }
}
