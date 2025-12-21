package com.va.v.v_app.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Cache configuration for Keycloak users
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    public static final String KEYCLOAK_USERS_CACHE = "keycloakUsers";

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(KEYCLOAK_USERS_CACHE);
        // Zero downtime - allow null values temporarily during refresh
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }
}
