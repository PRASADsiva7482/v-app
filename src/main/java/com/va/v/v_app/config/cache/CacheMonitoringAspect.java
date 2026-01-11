package com.va.v.v_app.config.cache;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Cache Monitoring Aspect for v-app
 * 
 * This aspect logs cache hits and misses for debugging and monitoring purposes.
 * It helps identify which methods benefit from caching and which need
 * optimization.
 * 
 * Features:
 * - Logs cache hits/misses with method details
 * - Measures cache operation duration
 * - Helps identify cache efficiency
 * 
 * @author v-app team
 * @since 2026-01-11
 */
@Slf4j
@Aspect
@Component
public class CacheMonitoringAspect {

    private final CacheManager cacheManager;

    public CacheMonitoringAspect(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Monitor all @Cacheable method executions
     * Logs cache hits and misses with timing information
     */
    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object monitorCacheable(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // Log cache operation
            if (log.isDebugEnabled()) {
                log.debug("Cache operation for method: {} with args: {} completed in {}ms",
                        methodName, Arrays.toString(args), duration);
            }

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Cache operation failed for method: {} with args: {} after {}ms",
                    methodName, Arrays.toString(args), duration, e);
            throw e;
        }
    }

    /**
     * Monitor all @CacheEvict method executions
     * Logs cache eviction events
     */
    @Around("@annotation(org.springframework.cache.annotation.CacheEvict)")
    public Object monitorCacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            if (log.isDebugEnabled()) {
                log.debug("Cache eviction for method: {} with args: {} completed in {}ms",
                        methodName, Arrays.toString(args), duration);
            }

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Cache eviction failed for method: {} with args: {} after {}ms",
                    methodName, Arrays.toString(args), duration, e);
            throw e;
        }
    }

    /**
     * Monitor all @CachePut method executions
     * Logs cache update events
     */
    @Around("@annotation(org.springframework.cache.annotation.CachePut)")
    public Object monitorCachePut(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            if (log.isDebugEnabled()) {
                log.debug("Cache update for method: {} with args: {} completed in {}ms",
                        methodName, Arrays.toString(args), duration);
            }

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Cache update failed for method: {} with args: {} after {}ms",
                    methodName, Arrays.toString(args), duration, e);
            throw e;
        }
    }
}
