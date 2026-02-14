package com.va.v.v_app.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.va.v.v_app.model.KeycloakAccessToken;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT Request Filter with Keycloak Validation
 * Intercepts every request to validate JWT token with Keycloak and set
 * authentication
 * Returns 401 Unauthorized if token validation fails
 */
@Slf4j
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final KeycloakTokenValidator keycloakTokenValidator;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Check for unauthenticated request bypass header (internal use only - not
        // documented in Swagger)
        final String unauthHeader = request.getHeader("x-unauth-request");
        if ("true".equalsIgnoreCase(unauthHeader)) {
            log.debug("Unauthenticated request bypass enabled for: {}", request.getRequestURI());

            // Create a bypass authentication token for internal use
            UsernamePasswordAuthenticationToken bypassToken = new UsernamePasswordAuthenticationToken(
                    "unauthenticated-user",
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_UNAUTH")));
            bypassToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(bypassToken);

            Long start = System.currentTimeMillis();
            filterChain.doFilter(request, response);
            log.info("Time Taken to complete API (unauth): {}", System.currentTimeMillis() - start);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // Extract JWT token from Authorization header
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            log.debug("JWT token found in Authorization header");
        } else {
            log.debug("No JWT token found in request to: {}", request.getRequestURI());
        }

        // Validate token with Keycloak and set authentication
        if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Validate token with Keycloak and get user details
            KeycloakAccessToken tokenDetails = keycloakTokenValidator.validateToken(jwt);

            if (tokenDetails == null || !"true".equalsIgnoreCase(tokenDetails.getActive())) {
                log.warn("Keycloak token validation failed");
                // Return 401 Unauthorized if Keycloak validation fails
                sendUnauthorizedResponse(response, "Token validation failed");
                return;
            }

            // Extract username from Keycloak response (try preferred_username first, then
            // username, then sub)
            username = tokenDetails.getPreferred_username();
            if (username == null || username.isEmpty()) {
                username = tokenDetails.getUsername();
            }
            if (username == null || username.isEmpty()) {
                username = tokenDetails.getSub(); // Use sub (user ID) as fallback
            }

            if (username == null || username.isEmpty()) {
                log.error("Could not extract username from Keycloak token response");
                sendUnauthorizedResponse(response, "Invalid token format");
                return;
            }

            log.debug("Token validated successfully with Keycloak for user: {}", username);

            // Create authentication token
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            // Store JWT and user details in request attributes for later use
            request.setAttribute("JWT_TOKEN", jwt);
            request.setAttribute("USERNAME", username);
            request.setAttribute("KEYCLOAK_TOKEN_DETAILS", tokenDetails);

            log.debug("Authentication set for user: {}", username);
        }

        Long start = System.currentTimeMillis();
        filterChain.doFilter(request, response);
        log.info("Time Taken to complete API: {}", System.currentTimeMillis() - start);
    }

    /**
     * Send 401 Unauthorized response
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
        response.getWriter().flush();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // Skip filter for public endpoints
        return path.startsWith("/swagger-ui") ||
                path.startsWith("/api-docs") ||
                path.startsWith("/actuator") ||
                path.equals("/error") ||
                path.startsWith("/public") ||
                path.startsWith("/api/v1/media/images/") ||
                path.startsWith("/api/v1/media/videos/") ||
                path.startsWith("/ws"); // WebSocket handshake handled by STOMP auth
    }
}
