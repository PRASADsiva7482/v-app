package com.va.v.v_app.core.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to populate MDC (Mapped Diagnostic Context) with request headers
 * This allows services to access headers via MDC throughout the request
 * lifecycle
 */
@Component
@Order(1)
@Slf4j
public class MDCFilter implements Filter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TXN_ID_HEADER = "txnId";
    private static final String USER_ID_HEADER = "userId";
    private static final String TEAM_ID_HEADER = "teamId";
    private static final String SESSION_ID_HEADER = "sessionId";
    private static final String USER_NAME_HEADER = "userName";
    private static final String ENTITY_ID_HEADER = "entityId";
    private static final String LANGUAGE_ID_HEADER = "languageId";
    private static final String SOURCE = "source";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // Populate MDC with request headers
            String authorization = httpRequest.getHeader(AUTHORIZATION_HEADER);
            if (authorization != null) {
                MDC.put(AUTHORIZATION_HEADER, authorization);
            }

            String txnId = httpRequest.getHeader(TXN_ID_HEADER);
            if (txnId == null || txnId.isEmpty()) {
                txnId = UUID.randomUUID().toString();
            }
            MDC.put(TXN_ID_HEADER, txnId);

            String userId = httpRequest.getHeader(USER_ID_HEADER);
            if (userId != null) {
                MDC.put(USER_ID_HEADER, userId);
            }

            String teamId = httpRequest.getHeader(TEAM_ID_HEADER);
            if (teamId != null) {
                MDC.put(TEAM_ID_HEADER, teamId);
            }

            String sessionId = httpRequest.getHeader(SESSION_ID_HEADER);
            if (sessionId != null) {
                MDC.put(SESSION_ID_HEADER, sessionId);
            }

            String userName = httpRequest.getHeader(USER_NAME_HEADER);
            if (userName != null) {
                MDC.put(USER_NAME_HEADER, userName);
            }

            String entityId = httpRequest.getHeader(ENTITY_ID_HEADER);
            if (entityId != null) {
                MDC.put(ENTITY_ID_HEADER, entityId);
            }

            String languageId = httpRequest.getHeader(LANGUAGE_ID_HEADER);
            if (languageId != null) {
                MDC.put(LANGUAGE_ID_HEADER, languageId);
            }

            String source = httpRequest.getHeader(SOURCE);
            if (source != null) {
                MDC.put(SOURCE, source);
            }

            log.debug("MDC populated - txnId: {}, userId: {}", txnId, userId);

            chain.doFilter(request, response);

        } finally {
            // Clear MDC after request processing
            MDC.clear();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("MDC Filter initialized");
    }

    @Override
    public void destroy() {
        log.info("MDC Filter destroyed");
    }
}
