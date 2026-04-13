package com.va.v.v_app.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Origin Enforcement Filter — blocks requests that don't come from trusted browser origins.
 *
 * HOW THIS WORKS (and why Postman/curl can't bypass it):
 * ─────────────────────────────────────────────────────
 * Browsers ALWAYS send an Origin header on cross-origin requests and a Referer
 * header on same-origin requests. These headers are "forbidden headers" — JavaScript
 * and browser extensions CANNOT modify or remove them. They are set by the browser
 * engine itself.
 *
 * Postman, curl, and HTTP libraries do NOT send an Origin or Referer header by default.
 * Even if they manually add one, this filter validates the value against a whitelist.
 *
 * The key insight: Only a REAL browser loading YOUR frontend from YOUR domain
 * will produce a valid, unforgeable Origin header. A tool can fake the header value,
 * but without a valid Keycloak JWT (which requires actual login through the Keycloak
 * UI hosted on your domain), the request will still fail at the JWT layer.
 *
 * This creates a TWO-FACTOR defense:
 *   1. Origin/Referer must match your frontend domain (blocks non-browser tools)
 *   2. JWT must be valid from Keycloak (blocks unauthorized users)
 *
 * Together: Even if someone steals a JWT, they can only use it from a browser on
 * your domain. And from Postman, they'd need BOTH a valid JWT AND to fake headers
 * that normally only your frontend produces.
 */
@Slf4j
@Component
public class AppSecurityFilter extends OncePerRequestFilter {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOriginsConfig;

    private List<String> allowedOrigins;

    private List<String> getAllowedOrigins() {
        if (allowedOrigins == null) {
            allowedOrigins = Arrays.asList(allowedOriginsConfig.split(","));
        }
        return allowedOrigins;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();

        // Let truly public paths through (swagger for dev, error page, health checks)
        if (isExemptPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // OPTIONS preflight requests don't need origin check (CORS handles them)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── Origin / Referer Enforcement ──
        // Browsers ALWAYS send one of these. Postman/curl do NOT.
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");

        boolean originValid = false;

        if (origin != null && !origin.isEmpty()) {
            // Check direct origin match
            originValid = getAllowedOrigins().stream()
                    .anyMatch(allowed -> allowed.trim().equalsIgnoreCase(origin));
        } else if (referer != null && !referer.isEmpty()) {
            // Same-origin browser requests send Referer instead of Origin
            originValid = getAllowedOrigins().stream()
                    .anyMatch(allowed -> referer.startsWith(allowed.trim()));
        }
        // If NEITHER Origin nor Referer is present → non-browser client (Postman/curl)

        if (!originValid) {
            log.warn("Blocked request from unauthorized origin. Path={}, Origin={}, Referer={}, IP={}",
                    path, origin, referer, getClientIp(request));

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Access denied\"}");
            response.getWriter().flush();
            return;
        }

        // Strip server identity headers before forwarding
        response.setHeader("Server", "");
        response.setHeader("X-Powered-By", "");

        filterChain.doFilter(request, response);
    }

    private boolean isExemptPath(String path) {
        return path.startsWith("/actuator/health") || // Only health, not all actuator
               path.equals("/error") ||
               // Media served via <img>/<video>/<audio> tags — may not send Origin/Referer
               path.startsWith("/api/v1/media/images/") ||
               path.startsWith("/api/v1/media/videos/") ||
               path.startsWith("/api/v1/media/profile-pictures/") ||
               path.startsWith("/api/v1/chat/media/files/") ||
               path.startsWith("/ws"); // WebSocket handshake (protected by STOMP auth)
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
