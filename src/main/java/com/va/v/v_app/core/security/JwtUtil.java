package com.va.v.v_app.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

/**
 * JWT Utility class for RS256 token validation (Keycloak)
 * Supports RSA public key verification for Keycloak JWT tokens
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${keycloak.public-key:}")
    private String keycloakPublicKey;

    private PublicKey publicKey;

    /**
     * Get RSA public key for token verification
     * This supports Keycloak's RS256 signed tokens
     */
    private PublicKey getPublicKey() {
        if (publicKey != null) {
            return publicKey;
        }

        try {
            if (keycloakPublicKey == null || keycloakPublicKey.isEmpty()) {
                log.warn("Keycloak public key not configured. Token validation will fail.");
                log.warn("Add 'keycloak.public-key' to your configuration");
                return null;
            }

            // Remove header/footer and whitespace
            String publicKeyPEM = keycloakPublicKey
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            // Decode base64
            byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);

            // Create public key
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            publicKey = keyFactory.generatePublic(keySpec);

            log.info("Keycloak public key loaded successfully for RS256 verification");
            return publicKey;

        } catch (Exception e) {
            log.error("Failed to load Keycloak public key: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize RSA public key", e);
        }
    }

    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            log.error("Exception in extractUsername() with cause = '{}' and exception = '{}'",
                    e.getCause() != null ? e.getCause().getMessage() : "null",
                    e.getMessage());
            throw e;
        }
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token (RS256 verification)
     */
    private Claims extractAllClaims(String token) {
        try {
            PublicKey key = getPublicKey();
            if (key == null) {
                throw new RuntimeException("Public key not available for token verification");
            }

            return Jwts.parser()
                    .verifyWith(key) // Use RSA public key for RS256 verification
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Error extracting claims from token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Check if token is expired
     */
    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Validate token
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token without username check
     */
    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract custom claim from token
     */
    public String extractCustomClaim(String token, String claimName) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get(claimName, String.class);
        } catch (Exception e) {
            log.error("Error extracting custom claim '{}': {}", claimName, e.getMessage());
            return null;
        }
    }

    /**
     * Extract preferred username (Keycloak specific)
     */
    public String extractPreferredUsername(String token) {
        return extractCustomClaim(token, "preferred_username");
    }

    /**
     * Extract email from token (Keycloak specific)
     */
    public String extractEmail(String token) {
        return extractCustomClaim(token, "email");
    }
}
