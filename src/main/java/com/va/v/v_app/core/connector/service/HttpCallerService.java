package com.va.v.v_app.core.connector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * HTTP Caller Service
 * Service for making HTTP API calls to external services
 * 
 * Uses Apache HttpClient 5 with connection pooling and configurable timeouts.
 * The httpClient.execute() method is the modern, recommended approach and is
 * NOT deprecated.
 */
@Slf4j
@Service
public class HttpCallerService {

    private final ObjectMapper objectMapper;
    private CloseableHttpClient httpClient; // Non-final, initialized in @PostConstruct

    // Configuration properties with defaults
    @Value("${http.client.connection.timeout:5000}")
    private int connectionTimeout;

    @Value("${http.client.socket.timeout:30000}")
    private int socketTimeout;

    @Value("${http.client.max.connections:100}")
    private int maxConnections;

    @Value("${http.client.max.connections.per.route:20}")
    private int maxConnectionsPerRoute;

    /**
     * Constructor - only sets the ObjectMapper
     * HttpClient is initialized in @PostConstruct after @Value properties are
     * injected
     */
    public HttpCallerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Initialize HttpClient after Spring has injected all @Value properties
     * This is called automatically by Spring after construction and dependency
     * injection
     */
    @PostConstruct
    public void init() {
        this.httpClient = createHttpClient();
        log.info("HttpCallerService initialized with connection pooling and timeouts: " +
                "connectionTimeout={}ms, socketTimeout={}ms, maxConnections={}, maxConnectionsPerRoute={}",
                connectionTimeout, socketTimeout, maxConnections, maxConnectionsPerRoute);
    }

    /**
     * Create a reusable HttpClient with connection pooling and timeouts
     * This is more efficient than creating a new client for each request
     */
    private CloseableHttpClient createHttpClient() {
        // Connection pool configuration
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);

        // Request configuration with timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(socketTimeout))
                .build();

        // Build HttpClient with custom configuration
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * Cleanup method to close HttpClient on application shutdown
     */
    @PreDestroy
    public void destroy() {
        try {
            if (httpClient != null) {
                httpClient.close();
                log.info("HttpClient closed successfully");
            }
        } catch (IOException e) {
            log.error("Error closing HttpClient: {}", e.getMessage(), e);
        }
    }

    /**
     * Build URL with query parameters
     * Properly encodes parameter values for URL safety
     * 
     * @param baseUrl Base URL without query parameters
     * @param params  Query parameters to append
     * @return Complete URL with query parameters
     */
    private String buildUrlWithParams(String baseUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }

        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        boolean firstParam = !baseUrl.contains("?");

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (firstParam) {
                urlBuilder.append("?");
                firstParam = false;
            } else {
                urlBuilder.append("&");
            }

            String key = entry.getKey();
            String value = entry.getValue();

            // URL encode the key and value
            try {
                urlBuilder.append(java.net.URLEncoder.encode(key, "UTF-8"))
                        .append("=")
                        .append(java.net.URLEncoder.encode(value, "UTF-8"));
            } catch (java.io.UnsupportedEncodingException e) {
                // UTF-8 is always supported, but handle just in case
                log.warn("Failed to encode URL parameter: {}={}", key, value);
                urlBuilder.append(key).append("=").append(value);
            }
        }

        String finalUrl = urlBuilder.toString();
        log.debug("Built URL with params: {}", finalUrl);
        return finalUrl;
    }

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

    // ========================================================================
    // Overloaded methods with query parameters support
    // ========================================================================

    /**
     * Make a GET request with query parameters
     * 
     * @param url          Base URL
     * @param params       Query parameters (e.g., ?key1=value1&key2=value2)
     * @param headers      HTTP headers
     * @param responseType Response class type
     * @return Parsed response object
     */
    public <T> T get(String url, Map<String, String> params, Map<String, String> headers, Class<T> responseType)
            throws IOException {
        String urlWithParams = buildUrlWithParams(url, params);
        return get(urlWithParams, headers, responseType);
    }

    /**
     * Make a POST request with query parameters
     * 
     * @param url          Base URL
     * @param params       Query parameters (e.g., ?key1=value1&key2=value2)
     * @param requestBody  Request body object
     * @param headers      HTTP headers
     * @param responseType Response class type
     * @return Parsed response object
     */
    public <T> T post(String url, Map<String, String> params, Object requestBody, Map<String, String> headers,
            Class<T> responseType) throws IOException {
        String urlWithParams = buildUrlWithParams(url, params);
        return post(urlWithParams, requestBody, headers, responseType);
    }

    /**
     * Make a PUT request with query parameters
     * 
     * @param url          Base URL
     * @param params       Query parameters (e.g., ?key1=value1&key2=value2)
     * @param requestBody  Request body object
     * @param headers      HTTP headers
     * @param responseType Response class type
     * @return Parsed response object
     */
    public <T> T put(String url, Map<String, String> params, Object requestBody, Map<String, String> headers,
            Class<T> responseType) throws IOException {
        String urlWithParams = buildUrlWithParams(url, params);
        return put(urlWithParams, requestBody, headers, responseType);
    }

    /**
     * Make a DELETE request with query parameters
     * 
     * @param url          Base URL
     * @param params       Query parameters (e.g., ?key1=value1&key2=value2)
     * @param headers      HTTP headers
     * @param responseType Response class type
     * @return Parsed response object
     */
    public <T> T delete(String url, Map<String, String> params, Map<String, String> headers, Class<T> responseType)
            throws IOException {
        String urlWithParams = buildUrlWithParams(url, params);
        return delete(urlWithParams, headers, responseType);
    }

    /**
     * Make a PATCH request with query parameters
     * 
     * @param url          Base URL
     * @param params       Query parameters (e.g., ?key1=value1&key2=value2)
     * @param requestBody  Request body object
     * @param headers      HTTP headers
     * @param responseType Response class type
     * @return Parsed response object
     */
    public <T> T patch(String url, Map<String, String> params, Object requestBody, Map<String, String> headers,
            Class<T> responseType) throws IOException {
        String urlWithParams = buildUrlWithParams(url, params);
        return patch(urlWithParams, requestBody, headers, responseType);
    }

    /**
     * Make a GET request with query parameters and return raw string response
     * 
     * @param url     Base URL
     * @param params  Query parameters (e.g., ?key1=value1&key2=value2)
     * @param headers HTTP headers
     * @return Raw response string
     */
    public String getRaw(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        String urlWithParams = buildUrlWithParams(url, params);
        return getRaw(urlWithParams, headers);
    }

    /**
     * Make a POST request with query parameters and return raw string response
     * 
     * @param url         Base URL
     * @param params      Query parameters (e.g., ?key1=value1&key2=value2)
     * @param requestBody Request body object
     * @param headers     HTTP headers
     * @return Raw response string
     */
    public String postRaw(String url, Map<String, String> params, Object requestBody, Map<String, String> headers)
            throws IOException {
        String urlWithParams = buildUrlWithParams(url, params);
        return postRaw(urlWithParams, requestBody, headers);
    }

    /**
     * Execute HTTP request and parse response
     * Uses the singleton httpClient for better resource management
     */
    private <T> T executeRequest(HttpUriRequestBase request, Class<T> responseType) throws IOException {
        // Note: We don't use try-with-resources for httpClient here because it's a
        // singleton
        // managed by the Spring container and closed in the @PreDestroy method
        try (CloseableHttpResponse response = httpClient.execute(request)) {

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
     * Uses the singleton httpClient for better resource management
     */
    private String executeRequestRaw(HttpUriRequestBase request) throws IOException {
        // Note: We don't use try-with-resources for httpClient here because it's a
        // singleton
        // managed by the Spring container and closed in the @PreDestroy method
        try (CloseableHttpResponse response = httpClient.execute(request)) {

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
