package com.va.v.v_app.config.payload;

import com.va.v.v_app.util.EncryptionUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Filter to handle request decryption and response encryption.
 * Toggleable via app.security.payload.encryption.enabled property.
 */
@Slf4j
public class PayloadEncryptionFilter extends OncePerRequestFilter {

    @Value("${app.security.payload.encryption.enabled:false}")
    private boolean isEncryptionEnabled;

    private static final List<String> SKIP_PATHS = Arrays.asList(
            "/api/v1/media/",
            "/swagger-ui",
            "/api-docs",
            "/actuator",
            "/error",
            "/v-app/api/v1/media/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean shouldSkip = SKIP_PATHS.stream().anyMatch(path::contains);

        if (!isEncryptionEnabled || shouldSkip) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Payload encryption/decryption active for: {} {}", request.getMethod(), path);

        // Decrypt Request Body
        HttpServletRequest wrappedRequest = request;
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            String contentType = request.getContentType();
            if (contentType != null && (contentType.contains("application/json") || contentType.contains("text/plain"))) {
                try {
                    String encryptedBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
                    if (encryptedBody != null && !encryptedBody.isEmpty()) {
                        log.debug("Decrypting request body for: {}", path);
                        String decryptedBody = EncryptionUtils.decrypt(encryptedBody);
                        wrappedRequest = new CachedBodyHttpServletRequest(request, decryptedBody);
                    }
                } catch (Exception e) {
                    log.error("Failed to decrypt request body for {}: {}", path, e.getMessage());
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("Invalid encrypted payload");
                    return;
                }
            }
        }

        // Encrypt Response Body
        ResponseContentWrapper responseWrapper = new ResponseContentWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, responseWrapper);
            
            byte[] responseData = responseWrapper.getContentAsByteArray();
            if (responseData.length > 0) {
                String contentType = response.getContentType();
                // Only encrypt JSON or Text responses
                if (contentType != null && (contentType.contains("application/json") || contentType.contains("text/plain"))) {
                    String originalResponse = new String(responseData, StandardCharsets.UTF_8);
                    log.debug("Encrypting response body for: {}", path);
                    String encryptedResponse = EncryptionUtils.encrypt(originalResponse);
                    
                    byte[] encryptedBytes = encryptedResponse.getBytes(StandardCharsets.UTF_8);
                    response.setContentType("text/plain"); // Set to text/plain for encrypted content
                    response.setContentLength(encryptedBytes.length);
                    response.getOutputStream().write(encryptedBytes);
                    response.getOutputStream().flush();
                } else {
                    response.getOutputStream().write(responseData);
                    response.getOutputStream().flush();
                }
            }
        } catch (Exception e) {
            log.error("Error during payload processing for {}: {}", path, e.getMessage());
            throw e;
        }
    }

    /**
     * Wrapper to cache the decrypted body for multiple reads.
     */
    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final String body;

        public CachedBodyHttpServletRequest(HttpServletRequest request, String body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
            return new ServletInputStream() {
                @Override
                public boolean isFinished() { return byteArrayInputStream.available() == 0; }
                @Override
                public boolean isReady() { return true; }
                @Override
                public void setReadListener(ReadListener readListener) {}
                @Override
                public int read() { return byteArrayInputStream.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(this.getInputStream(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Wrapper to capture response content.
     */
    private static class ResponseContentWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream capture = new ByteArrayOutputStream();
        private ServletOutputStream output;
        private PrintWriter writer;

        public ResponseContentWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() {
            if (writer != null) throw new IllegalStateException("getWriter() has already been called");
            if (output == null) {
                output = new ServletOutputStream() {
                    @Override
                    public boolean isReady() { return true; }
                    @Override
                    public void setWriteListener(WriteListener writeListener) {}
                    @Override
                    public void write(int b) { capture.write(b); }
                    @Override
                    public void write(byte[] b, int off, int len) { capture.write(b, off, len); }
                };
            }
            return output;
        }

        @Override
        public PrintWriter getWriter() {
            if (output != null) throw new IllegalStateException("getOutputStream() has already been called");
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(capture, StandardCharsets.UTF_8), true);
            }
            return writer;
        }

        @Override
        public void flushBuffer() throws IOException {
            if (writer != null) writer.flush();
            if (output != null) output.flush();
        }

        public byte[] getContentAsByteArray() {
            try {
                flushBuffer();
            } catch (IOException e) {
                log.error("Error flushing buffer", e);
            }
            return capture.toByteArray();
        }
    }
}
