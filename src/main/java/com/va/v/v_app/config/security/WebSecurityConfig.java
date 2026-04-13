package com.va.v.v_app.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


import java.util.Arrays;
import java.util.List;

/**
 * Web Security Configuration — Hardened.
 *
 * Security layers (in order of filter execution):
 *   1. AppSecurityFilter   → Blocks requests without valid browser Origin/Referer
 *   2. JwtRequestFilter    → Validates Keycloak JWT token
 *   3. Spring Security     → Authorizes endpoint access
 *
 * Security headers enforced:
 *   - Content-Security-Policy (CSP)
 *   - X-Frame-Options: DENY
 *   - X-Content-Type-Options: nosniff
 *   - X-XSS-Protection: 1; mode=block
 *   - Strict-Transport-Security (HSTS)
 *   - Referrer-Policy: strict-origin-when-cross-origin
 *   - Permissions-Policy: restrictive defaults
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public JwtRequestFilter jwtRequestFilter(KeycloakTokenValidator keycloakTokenValidator) {
        return new JwtRequestFilter(keycloakTokenValidator);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                    KeycloakTokenValidator keycloakTokenValidator,
                    AppSecurityFilter appSecurityFilter) throws Exception {
        log.info("Configuring hardened security filter chain");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // ── Security Headers ──
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                        "script-src 'self'; " +
                                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                                        "font-src 'self' https://fonts.gstatic.com; " +
                                        "img-src 'self' data: blob:; " +
                                        "media-src 'self' blob:; " +
                                        "connect-src 'self' ws: wss:; " +
                                        "frame-ancestors 'none'; " +
                                        "base-uri 'self'; " +
                                        "form-action 'self';"
                                ))
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(org.springframework.security.config.Customizer.withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .preload(true))
                        .referrerPolicy(referrer -> referrer
                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                                        .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicy(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=(), payment=()"))
                )
                // ── Endpoint Authorization ──
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",   // Only health endpoint, NOT all actuator
                                "/error",
                                // Media served via <img>/<video> tags — can't add JWT headers
                                // Protected by Origin enforcement filter instead
                                "/api/v1/media/images/**",
                                "/api/v1/media/videos/**",
                                "/api/v1/media/profile-pictures/**",
                                "/api/v1/chat/media/files/**",
                                "/ws/**"
                        ).permitAll()
                        // Everything else requires authentication
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // ── Filter Chain Order ──
                // 1) Origin enforcement (blocks Postman/curl)
                .addFilterBefore(appSecurityFilter, UsernamePasswordAuthenticationFilter.class)
                // 2) JWT validation (blocks unauthorized users)
                .addFilterBefore(jwtRequestFilter(keycloakTokenValidator),
                                UsernamePasswordAuthenticationFilter.class)
                // ── Disable server info in error responses ──
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Access denied\"}");
                        })
                );

        log.info("Hardened security filter chain configured");
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        log.info("CORS configured with allowed origins: {}", origins);

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"));

        configuration.setExposedHeaders(List.of("Content-Type"));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Prevent Tomcat from auto-registering AppSecurityFilter as a servlet filter.
     * It must ONLY run inside the Spring Security filter chain (via addFilterBefore).
     * Without this, Tomcat tries GenericFilterBean.init() on the CGLIB proxy,
     * which crashes with NullPointerException on the logger field.
     */
    @Bean
    public FilterRegistrationBean<AppSecurityFilter> appSecurityFilterRegistration(AppSecurityFilter filter) {
        FilterRegistrationBean<AppSecurityFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Prevent Tomcat from auto-registering JwtRequestFilter as a servlet filter.
     * It must ONLY run inside the Spring Security filter chain.
     */
    @Bean
    public FilterRegistrationBean<JwtRequestFilter> jwtRequestFilterRegistration(JwtRequestFilter filter) {
        FilterRegistrationBean<JwtRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
