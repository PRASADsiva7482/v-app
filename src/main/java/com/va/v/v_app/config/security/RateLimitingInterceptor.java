package com.va.v.v_app.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting Interceptor with per-IP throttling and auto-cleanup.
 *
 * Prevents brute-force attacks, credential stuffing, and API abuse.
 * Uses a sliding window approach per minute.
 *
 * Configurable via:
 *   app.security.rate-limit.max-requests=120
 *   app.security.rate-limit.login-max-requests=10
 */
@Slf4j
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    @Value("${app.security.rate-limit.max-requests:120}")
    private int maxRequestsPerMinute;

    @Value("${app.security.rate-limit.login-max-requests:10}")
    private int loginMaxRequestsPerMinute;

    // IP → RequestCounter
    private final Map<String, RequestCounter> counters = new ConcurrentHashMap<>();
    private long lastCleanup = System.currentTimeMillis();
    private static final long CLEANUP_INTERVAL_MS = 5 * 60 * 1000; // 5 min

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        long currentMinute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());

        // Periodic cleanup of stale entries to prevent memory leak
        cleanupStaleEntries(currentMinute);

        RequestCounter counter = counters.computeIfAbsent(clientIp, k -> new RequestCounter(currentMinute));

        if (counter.minute != currentMinute) {
            counter.reset(currentMinute);
        }

        // Apply stricter limit for auth-sensitive endpoints
        String path = request.getServletPath();
        int limit = isAuthEndpoint(path) ? loginMaxRequestsPerMinute : maxRequestsPerMinute;

        if (counter.count.incrementAndGet() > limit) {
            log.warn("Rate limit exceeded for IP: {} on path: {}. Count: {}/{}", clientIp, path, counter.count.get(), limit);

            response.setStatus(429);
            response.setContentType("application/json");
            // Generic message — don't reveal the actual limit
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
            response.getWriter().flush();

            // Add Retry-After header (in seconds)
            response.setHeader("Retry-After", "60");
            return false;
        }

        return true;
    }

    private boolean isAuthEndpoint(String path) {
        return path.contains("/login") ||
               path.contains("/token") ||
               path.contains("/auth") ||
               path.contains("/password");
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim(); // First IP is the real client
        }
        return request.getRemoteAddr();
    }

    private void cleanupStaleEntries(long currentMinute) {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
            lastCleanup = now;
            counters.entrySet().removeIf(entry -> entry.getValue().minute < currentMinute - 2);
            log.debug("Rate limiter cleanup: {} entries remaining", counters.size());
        }
    }

    private static class RequestCounter {
        volatile long minute;
        final AtomicInteger count = new AtomicInteger(0);

        RequestCounter(long minute) {
            this.minute = minute;
        }

        void reset(long minute) {
            this.minute = minute;
            this.count.set(0);
        }
    }
}
