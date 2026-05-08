package com.va.v.v_app.core.security;

import com.va.v.v_app.iam.model.KeycloakAccessToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Keycloak Token Validator
 * Validates JWT tokens against Keycloak's token introspection endpoint
 */
@Slf4j
@Component
public class KeycloakTokenValidator {

    @Value("${keycloak.enabled:true}")
    private boolean keycloakEnabled;

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Validates a JWT token with Keycloak
     * 
     * @param token JWT token to validate (without "Bearer " prefix)
     * @return KeycloakAccessToken if token is valid and active, null otherwise
     */
    public KeycloakAccessToken validateToken(String token) {
        if (!keycloakEnabled) {
            log.error("SECURITY: Keycloak validation is disabled! This must NEVER happen in production.");
            // Do NOT return a mock token — reject the request
            return null;
        }

        if (StringUtils.isEmpty(token)) {
            log.warn("Cannot validate empty token");
            return null;
        }

        try {
            log.debug("Validating token with Keycloak introspection endpoint");

            String introspectEndpoint = keycloakServerUrl + "/realms/" + realm
                    + "/protocol/openid-connect/token/introspect";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("client_id", clientId);
            requestBody.add("client_secret", clientSecret);
            requestBody.add("token", token);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

            long startTime = System.currentTimeMillis();
            ResponseEntity<KeycloakAccessToken> response = restTemplate.exchange(
                    introspectEndpoint,
                    HttpMethod.POST,
                    request,
                    KeycloakAccessToken.class);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Keycloak token validation took {}ms", duration);

            KeycloakAccessToken tokenResponse = response.getBody();

            if (tokenResponse == null) {
                log.warn("No response from Keycloak introspection endpoint");
                return null;
            }

            // Check if token is active
            boolean isActive = "true".equalsIgnoreCase(tokenResponse.getActive());

            if (isActive) {
                log.debug("Token is valid and active for user: {}", tokenResponse.getPreferred_username());
                return tokenResponse;
            } else {
                log.warn("Token is not active");
                return null;
            }

        } catch (RestClientException e) {
            log.error("Error communicating with Keycloak: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error during token validation: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Validates a JWT token and returns the full token response from Keycloak
     * 
     * @param token JWT token to validate (without "Bearer " prefix)
     * @return KeycloakAccessToken object with validation details, or null if
     *         validation fails
     */
    public KeycloakAccessToken validateTokenWithDetails(String token) {
        if (!keycloakEnabled) {
            log.debug("Keycloak validation is disabled");
            return null;
        }

        if (StringUtils.isEmpty(token)) {
            log.warn("Cannot validate empty token");
            return null;
        }

        try {
            log.debug("Validating token with Keycloak introspection endpoint");

            String introspectEndpoint = keycloakServerUrl + "/realms/" + realm
                    + "/protocol/openid-connect/token/introspect";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("client_id", clientId);
            requestBody.add("client_secret", clientSecret);
            requestBody.add("token", token);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

            long startTime = System.currentTimeMillis();
            ResponseEntity<KeycloakAccessToken> response = restTemplate.exchange(
                    introspectEndpoint,
                    HttpMethod.POST,
                    request,
                    KeycloakAccessToken.class);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Keycloak token validation took {}ms", duration);

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error communicating with Keycloak: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error during token validation: {}", e.getMessage(), e);
            return null;
        }
    }
}
