package com.va.v.v_app.config.security;

import lombok.extern.slf4j.Slf4j;
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
 * Web Security Configuration
 * Configures Spring Security with JWT authentication
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

        /**
         * JWT Request Filter Bean
         */
        @Bean
        public JwtRequestFilter jwtRequestFilter(KeycloakTokenValidator keycloakTokenValidator) {
                log.info("Creating JwtRequestFilter bean with Keycloak validation");
                return new JwtRequestFilter(keycloakTokenValidator);
        }

        /**
         * Service ID Decryption Filter Bean
         */
        @Bean
        public ServiceIdDecryptionFilter serviceIdDecryptionFilter() {
                log.info("Creating ServiceIdDecryptionFilter bean");
                return new ServiceIdDecryptionFilter();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http,
                        KeycloakTokenValidator keycloakTokenValidator) throws Exception {
                log.info("Configuring security filter chain");

                http
                                // Disable CSRF for stateless API
                                .csrf(AbstractHttpConfigurer::disable)

                                // Configure CORS
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // Configure authorization
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints - no authentication required
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/api-docs/**",
                                                                "/actuator/**",
                                                                "/error",
                                                                "/public/**",
                                                                "/api/v1/media/images/**", // Allow public access to
                                                                                           // images
                                                                "/api/v1/media/videos/**", // Allow public access to
                                                                                           // videos
                                                                "/api/test/**" // Test endpoints for development
                                                ).permitAll()

                                                // All other endpoints require authentication
                                                .anyRequest().authenticated())

                                // Stateless session management
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // Add custom filters
                                .addFilterBefore(serviceIdDecryptionFilter(),
                                                UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(jwtRequestFilter(keycloakTokenValidator),
                                                UsernamePasswordAuthenticationFilter.class);

                log.info("Security filter chain configured successfully");
                return http.build();
        }

        /**
         * CORS Configuration
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Allow all origins in development (configure properly for production)
                configuration.setAllowedOriginPatterns(List.of("*"));

                // Allow common HTTP methods
                configuration.setAllowedMethods(Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

                // Allow common headers
                configuration.setAllowedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "X-Service-Id",
                                "X-Encrypted-Service-Id",
                                "X-Requested-With",
                                "Accept",
                                "Origin",
                                "Access-Control-Request-Method",
                                "Access-Control-Request-Headers"));

                // Expose headers
                configuration.setExposedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "X-Service-Id"));

                // Allow credentials
                configuration.setAllowCredentials(true);

                // Max age
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }
}
