package com.va.v.v_app.core.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT Configuration Properties
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfigProperties {

    /**
     * JWT secret key (minimum 256 bits / 32 characters)
     */
    private String secret = "your-256-bit-secret-key-change-this-in-production-minimum-32-characters";

    /**
     * JWT expiration time in milliseconds (default: 24 hours)
     */
    private Long expiration = 86400000L;

    /**
     * JWT issuer
     */
    private String issuer = "v-app";

    /**
     * Enable JWT validation
     */
    private boolean enabled = true;
}
