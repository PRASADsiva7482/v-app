package com.va.v.v_app.core.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Cache Configuration for v-app
 * 
 * This configuration enables Redis as a caching layer using the Cache-Aside
 * pattern.
 * Redis sits between Spring Boot and MySQL, improving read performance without
 * modifying existing datasource configurations.
 * 
 * Key Features:
 * - Cache-Aside (Lazy Loading) pattern
 * - Domain-based cache names (crm:*, billing:*, keycloak:*)
 * - Configurable TTL per cache
 * - Safe error handling (fallback to DB on cache failures)
 * - JSON serialization for complex objects
 * 
 * @author v-app team
 * @since 2026-01-11
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = true)
public class RedisCacheConfig implements CachingConfigurer {

    @Value("${spring.cache.redis.time-to-live:600000}")
    private Long defaultTtl;

    /**
     * Configure ObjectMapper for Redis JSON serialization
     * Supports Java 8 time types and polymorphic type handling
     */
    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register JavaTimeModule for Java 8 date/time support (LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());

        // Disable writing dates as timestamps (write as ISO-8601 strings instead)
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Enable polymorphic type handling for proper deserialization
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        return mapper;
    }

    /**
     * Primary RedisTemplate bean for general Redis operations
     * Uses String keys and JSON value serialization
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serialization for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Use JSON serialization for values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();

        log.info("RedisTemplate configured with JSON serialization");
        return template;
    }

    /**
     * Configure CacheManager with domain-specific cache configurations
     * 
     * Cache Names:
     * - crm:customer - Customer data cache (10 min TTL)
     * - crm:user - User profile cache (10 min TTL)
     * - billing:invoice - Invoice cache (5 min TTL)
     * - billing:payment - Payment cache (5 min TTL)
     * - keycloak:user - Keycloak user cache (15 min TTL)
     * - keycloak:role - Keycloak role cache (30 min TTL)
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = createCacheConfig(Duration.ofMillis(defaultTtl));

        // Domain-specific cache configurations with custom TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // CRM Cache Configurations
        cacheConfigurations.put("crm:customer", createCacheConfig(Duration.ofMinutes(10)));
        cacheConfigurations.put("crm:user", createCacheConfig(Duration.ofMinutes(10)));
        cacheConfigurations.put("crm:profile", createCacheConfig(Duration.ofMinutes(10)));
        cacheConfigurations.put("crm:post", createCacheConfig(Duration.ofMinutes(5)));
        cacheConfigurations.put("crm:feed", createCacheConfig(Duration.ofMinutes(3)));
        cacheConfigurations.put("crm:hashtag", createCacheConfig(Duration.ofMinutes(15)));

        // Billing Cache Configurations
        cacheConfigurations.put("billing:invoice", createCacheConfig(Duration.ofMinutes(5)));
        cacheConfigurations.put("billing:payment", createCacheConfig(Duration.ofMinutes(5)));
        cacheConfigurations.put("billing:transaction", createCacheConfig(Duration.ofMinutes(5)));

        // Keycloak Cache Configurations (less volatile data = longer TTL)
        cacheConfigurations.put("keycloak:user", createCacheConfig(Duration.ofMinutes(15)));
        cacheConfigurations.put("keycloak:role", createCacheConfig(Duration.ofMinutes(30)));
        cacheConfigurations.put("keycloak:permission", createCacheConfig(Duration.ofMinutes(30)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();

        log.info("RedisCacheManager configured with {} domain-specific caches",
                cacheConfigurations.size());

        return cacheManager;
    }

    /**
     * Create cache configuration with custom TTL
     */
    private RedisCacheConfiguration createCacheConfig(Duration ttl) {
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));
    }

    /**
     * Custom error handler for cache operations
     * Ensures application continues to work even if Redis is down
     * Falls back to database queries on cache errors
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache,
                    Object key) {
                log.error("Cache GET error for cache: {}, key: {}. Falling back to database.",
                        cache.getName(), key, exception);
                // Application continues - falls back to database
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache,
                    Object key, Object value) {
                log.error("Cache PUT error for cache: {}, key: {}. Data will not be cached.",
                        cache.getName(), key, exception);
                // Application continues - just won't cache this data
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache,
                    Object key) {
                log.error("Cache EVICT error for cache: {}, key: {}. Cache may contain stale data.",
                        cache.getName(), key, exception);
                // Application continues - but cache might be stale
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.error("Cache CLEAR error for cache: {}. Cache may contain stale data.",
                        cache.getName(), exception);
                // Application continues - but cache might be stale
            }
        };
    }
}
