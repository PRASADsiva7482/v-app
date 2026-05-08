package com.va.v.v_app.core.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;

/**
 * Enhanced Cache Configuration with Redis support
 * Falls back to in-memory cache if Redis is not available
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

        public static final String KEYCLOAK_USERS_CACHE = "keycloakUsers";
        public static final String USER_PROFILES_CACHE = "userProfiles";
        public static final String POSTS_CACHE = "posts";
        public static final String FEEDS_CACHE = "feeds";

    /**
     * Redis-based cache manager (Primary - used when Redis is available)
     * Configured with JavaTimeModule and polymorphic typing for proper serialization
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // Create ObjectMapper with JavaTimeModule for LocalDateTime support
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Enable polymorphic type handling to store @class metadata as JSON property
        // Using As.PROPERTY instead of As.WRAPPER_ARRAY for compatibility
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        
        // Create serializer with the configured ObjectMapper
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL: 30 minutes
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(KEYCLOAK_USERS_CACHE,
                        defaultConfig.entryTtl(Duration.ofHours(2))) // Keycloak users: 2 hours
                .withCacheConfiguration(USER_PROFILES_CACHE,
                        defaultConfig.entryTtl(Duration.ofHours(1))) // User profiles: 1 hour
                .withCacheConfiguration(POSTS_CACHE,
                        defaultConfig.entryTtl(Duration.ofMinutes(15))) // Posts: 15 minutes
                .withCacheConfiguration(FEEDS_CACHE,
                        defaultConfig.entryTtl(Duration.ofMinutes(5))) // Feeds: 5 minutes
                .transactionAware()
                .build();
    }

        /**
         * Fallback in-memory cache manager (when Redis is not available)
         */
        @Bean
        @ConditionalOnProperty(name = "spring.data.redis.host", matchIfMissing = true, havingValue = "false")
        public CacheManager inMemoryCacheManager() {
                ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                                KEYCLOAK_USERS_CACHE,
                                USER_PROFILES_CACHE,
                                POSTS_CACHE,
                                FEEDS_CACHE);
                cacheManager.setAllowNullValues(false);
                return cacheManager;
        }
}
