package com.va.v.v_app.iam.service;

import com.va.v.v_app.iam.model.KeycloakAccessToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class KeycloakRequestMaker {

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${keycloak.grant-type:urn:ietf:params:oauth:grant-type:uma-ticket}")
    private String grantType;

    @Value("${keycloak.audience:}")
    private String audience;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Initiates SSO request to Keycloak to get access token with permissions
     */
    public KeycloakAccessToken initiateSSOReq() {
        log.info("Initiating SSO Call with token to authenticate user");

        String tokenEndpoint = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Get the bearer token from MDC (set by filter/interceptor)
        String bearerToken = MDC.get(AUTHORIZATION_HEADER);
        if (StringUtils.isNotEmpty(bearerToken)) {
            headers.set(AUTHORIZATION_HEADER, bearerToken);
        }

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", grantType);
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);

        if (StringUtils.isNotEmpty(audience)) {
            requestBody.add("audience", audience);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<KeycloakAccessToken> response = restTemplate.exchange(
                    tokenEndpoint,
                    HttpMethod.POST,
                    request,
                    KeycloakAccessToken.class);

            KeycloakAccessToken token = response.getBody();

            if (token == null || StringUtils.isEmpty(token.getAccess_token())) {
                throw new RuntimeException("Unable to authenticate user - no access token received");
            }

            log.info("Token Details Received successfully");
            return token;

        } catch (Exception e) {
            log.error("Error during Keycloak authentication", e);
            throw new RuntimeException("Authentication service unavailable", e);
        }
    }

    /**
     * Validates an existing token with Keycloak
     */
    public KeycloakAccessToken initiateSSOForTokenValidation() {
        log.info("Initiating SSO Call for token validation");

        String introspectEndpoint = keycloakServerUrl + "/realms/" + realm
                + "/protocol/openid-connect/token/introspect";

        String bearerToken = MDC.get(AUTHORIZATION_HEADER);
        if (StringUtils.isEmpty(bearerToken)) {
            throw new RuntimeException("No token available for validation");
        }

        // Extract token without "Bearer " prefix
        String token = bearerToken;
        if (bearerToken.startsWith("Bearer ")) {
            token = bearerToken.substring(7);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("token", token);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            long startTime = System.currentTimeMillis();
            ResponseEntity<KeycloakAccessToken> response = restTemplate.exchange(
                    introspectEndpoint,
                    HttpMethod.POST,
                    request,
                    KeycloakAccessToken.class);

            log.info("Process Time Taken For: Validate Token = " + (System.currentTimeMillis() - startTime));

            KeycloakAccessToken tokenResponse = response.getBody();

            if (tokenResponse == null || StringUtils.isEmpty(tokenResponse.getActive())) {
                throw new RuntimeException("Unable to validate token");
            }

            return tokenResponse;

        } catch (Exception e) {
            log.error("Error during token validation", e);
            throw new RuntimeException("Token validation service unavailable", e);
        }
    }
}
