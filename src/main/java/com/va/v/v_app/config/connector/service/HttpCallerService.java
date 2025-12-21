package com.va.v.v_app.config.connector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * HTTP Caller Service
 * Service for making HTTP API calls to external services
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HttpCallerService {

    private final ObjectMapper objectMapper;

    /**
     * Make a GET request
     */
    public <T> T get(String url, Map<String, String> headers, Class<T> responseType) throws IOException {
        log.debug("Making GET request to: {}", url);

        HttpGet httpGet = new HttpGet(url);
        addHeaders(httpGet, headers);

        return executeRequest(httpGet, responseType);
    }

    /**
     * Make a POST request
     */
    public <T> T post(String url, Object requestBody, Map<String, String> headers, Class<T> responseType)
            throws IOException {
        log.debug("Making POST request to: {}", url);

        HttpPost httpPost = new HttpPost(url);
        addHeaders(httpPost, headers);

        if (requestBody != null) {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            httpPost.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        }

        return executeRequest(httpPost, responseType);
    }

    /**
     * Make a PUT request
     */
    public <T> T put(String url, Object requestBody, Map<String, String> headers, Class<T> responseType)
            throws IOException {
        log.debug("Making PUT request to: {}", url);

        HttpPut httpPut = new HttpPut(url);
        addHeaders(httpPut, headers);

        if (requestBody != null) {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            httpPut.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        }

        return executeRequest(httpPut, responseType);
    }

    /**
     * Make a DELETE request
     */
    public <T> T delete(String url, Map<String, String> headers, Class<T> responseType) throws IOException {
        log.debug("Making DELETE request to: {}", url);

        HttpDelete httpDelete = new HttpDelete(url);
        addHeaders(httpDelete, headers);

        return executeRequest(httpDelete, responseType);
    }

    /**
     * Make a PATCH request
     */
    public <T> T patch(String url, Object requestBody, Map<String, String> headers, Class<T> responseType)
            throws IOException {
        log.debug("Making PATCH request to: {}", url);

        HttpPatch httpPatch = new HttpPatch(url);
        addHeaders(httpPatch, headers);

        if (requestBody != null) {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            httpPatch.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        }

        return executeRequest(httpPatch, responseType);
    }

    /**
     * Make a GET request and return raw string response
     */
    public String getRaw(String url, Map<String, String> headers) throws IOException {
        log.debug("Making GET request (raw) to: {}", url);

        HttpGet httpGet = new HttpGet(url);
        addHeaders(httpGet, headers);

        return executeRequestRaw(httpGet);
    }

    /**
     * Make a POST request and return raw string response
     */
    public String postRaw(String url, Object requestBody, Map<String, String> headers) throws IOException {
        log.debug("Making POST request (raw) to: {}", url);

        HttpPost httpPost = new HttpPost(url);
        addHeaders(httpPost, headers);

        if (requestBody != null) {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            httpPost.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        }

        return executeRequestRaw(httpPost);
    }

    /**
     * Execute HTTP request and parse response
     */
    private <T> T executeRequest(HttpUriRequestBase request, Class<T> responseType) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(request)) {

            int statusCode = response.getCode();
            String responseBody = "";

            try {
                responseBody = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
            } catch (ParseException e) {
                log.error("Error parsing response entity: {}", e.getMessage());
                throw new IOException("Failed to parse response", e);
            }

            log.debug("Response status: {}", statusCode);
            log.debug("Response body: {}", responseBody);

            if (statusCode >= 200 && statusCode < 300) {
                if (responseType == String.class) {
                    return responseType.cast(responseBody);
                }
                return objectMapper.readValue(responseBody, responseType);
            } else {
                log.error("HTTP request failed with status: {} and body: {}", statusCode, responseBody);
                throw new IOException("HTTP request failed with status: " + statusCode);
            }
        }
    }

    /**
     * Execute HTTP request and return raw response
     */
    private String executeRequestRaw(HttpUriRequestBase request) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(request)) {

            int statusCode = response.getCode();
            String responseBody = "";

            try {
                responseBody = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
            } catch (ParseException e) {
                log.error("Error parsing response entity: {}", e.getMessage());
                throw new IOException("Failed to parse response", e);
            }

            log.debug("Response status: {}", statusCode);

            if (statusCode >= 200 && statusCode < 300) {
                return responseBody;
            } else {
                log.error("HTTP request failed with status: {} and body: {}", statusCode, responseBody);
                throw new IOException("HTTP request failed with status: " + statusCode);
            }
        }
    }

    /**
     * Add headers to request
     */
    private void addHeaders(HttpUriRequestBase request, Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            headers.forEach((key, value) -> {
                request.addHeader(key, value);
                log.debug("Added header: {} = {}", key, value);
            });
        }

        // Add default Content-Type if not present
        if (headers == null || !headers.containsKey("Content-Type")) {
            request.addHeader("Content-Type", "application/json");
        }
    }

    /**
     * Build authorization header with Bearer token
     */
    public static Map<String, String> buildAuthHeaders(String token) {
        return Map.of("Authorization", "Bearer " + token);
    }

    /**
     * Build headers with authorization and custom headers
     */
    public static Map<String, String> buildHeaders(String token, Map<String, String> customHeaders) {
        Map<String, String> headers = new java.util.HashMap<>(Map.of("Authorization", "Bearer " + token));
        if (customHeaders != null) {
            headers.putAll(customHeaders);
        }
        return headers;
    }
}
