package com.va.v.v_app.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;

/**
 * Service ID Decryption Filter
 * Decrypts service ID from request header and stores it in request attributes
 */
@Slf4j
public class ServiceIdDecryptionFilter extends OncePerRequestFilter {

    @Autowired(required = false)
    private StringEncryptor stringEncryptor;

    private static final String SERVICE_ID_HEADER = "X-Service-Id";
    private static final String ENCRYPTED_SERVICE_ID_HEADER = "X-Encrypted-Service-Id";
    private static final String SERVICE_ID_ATTRIBUTE = "SERVICE_ID";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String serviceId = null;

        // Try to get plain service ID first
        String plainServiceId = request.getHeader(SERVICE_ID_HEADER);
        if (StringUtils.hasText(plainServiceId)) {
            serviceId = plainServiceId;
            log.debug("Plain service ID found: {}", serviceId);
        } else {
            // Try to get encrypted service ID
            String encryptedServiceId = request.getHeader(ENCRYPTED_SERVICE_ID_HEADER);
            if (StringUtils.hasText(encryptedServiceId)) {
                try {
                    // Decrypt the service ID
                    serviceId = decryptServiceId(encryptedServiceId);
                    log.debug("Decrypted service ID: {}", serviceId);
                } catch (Exception e) {
                    log.error("Failed to decrypt service ID: {}", e.getMessage());
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid encrypted service ID");
                    return;
                }
            }
        }

        // Store service ID in request attribute if found
        if (serviceId != null) {
            request.setAttribute(SERVICE_ID_ATTRIBUTE, serviceId);
            log.debug("Service ID stored in request attribute: {}", serviceId);
        } else {
            log.debug("No service ID found in request headers");
        }

        Long start = System.currentTimeMillis();
        filterChain.doFilter(request, response);
        log.info("Time Taken to complete API {}", System.currentTimeMillis() - start);
    }

    /**
     * Decrypt service ID
     * Supports both Base64 encoded and plain encrypted strings
     */
    private String decryptServiceId(String encryptedServiceId) {
        if (stringEncryptor == null) {
            log.warn("StringEncryptor not available, returning encrypted service ID as-is");
            return encryptedServiceId;
        }

        try {
            // Try to decode from Base64 first
            String decodedString;
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(encryptedServiceId);
                decodedString = new String(decodedBytes);
            } catch (IllegalArgumentException e) {
                // Not Base64 encoded, use as is
                decodedString = encryptedServiceId;
            }

            // Decrypt using Jasypt
            return stringEncryptor.decrypt(decodedString);
        } catch (Exception e) {
            log.error("Error decrypting service ID: {}", e.getMessage());
            throw new RuntimeException("Failed to decrypt service ID", e);
        }
    }

    /**
     * Get service ID from request
     * Utility method to retrieve service ID from request attributes
     */
    public static String getServiceId(HttpServletRequest request) {
        Object serviceId = request.getAttribute(SERVICE_ID_ATTRIBUTE);
        return serviceId != null ? serviceId.toString() : null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // Skip filter for public endpoints
        return path.startsWith("/swagger-ui") ||
                path.startsWith("/api-docs") ||
                path.startsWith("/actuator") ||
                path.equals("/error") ||
                path.startsWith("/public");
    }
}
