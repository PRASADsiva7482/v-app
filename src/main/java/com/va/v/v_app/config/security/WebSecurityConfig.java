package com.va.v.v_app.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 * 
 * Fixes applied:
 * - B-13: CORS now uses configurable allowed origins instead of wildcard "*"
 * - B-14: Removed /api/test/** from permitted endpoints
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

        /**
         * B-13: CORS origins from config, with safe defaults.
         * Set in application.properties:
         * cors.allowed-origins=http://localhost:3000,http://localhost:5173
         */
        @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
        private String allowedOrigins;

        @Bean
        public JwtRequestFilter jwtRequestFilter(KeycloakTokenValidator keycloakTokenValidator) {
                log.info("Creating JwtRequestFilter bean with Keycloak validation");
                return new JwtRequestFilter(keycloakTokenValidator);
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http,
                        KeycloakTokenValidator keycloakTokenValidator) throws Exception {
                log.info("Configuring security filter chain");

                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/api-docs/**",
                                                                "/actuator/**",
                                                                "/error",
                                                                "/public/**",
                                                                "/api/v1/media/images/**",
                                                                "/api/v1/media/videos/**",
                                                                "/api/v1/chat/media/files/**",
                                                                "/ws/**"
                                                // B-14: REMOVED "/api/test/**" — test endpoints
                                                // should NOT be exposed in production
                                                ).permitAll()
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .addFilterBefore(jwtRequestFilter(keycloakTokenValidator),
                                                UsernamePasswordAuthenticationFilter.class);

                log.info("Security filter chain configured successfully");
                return http.build();
        }

        /**
         * B-13: CORS configuration with explicit allowed origins.
         * In production, set cors.allowed-origins to your actual frontend domain(s).
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // B-13: Parse allowed origins from config instead of wildcard
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
                                "Origin",
                                "Access-Control-Request-Method",
                                "Access-Control-Request-Headers"));

                configuration.setExposedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type"));

                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }
}
