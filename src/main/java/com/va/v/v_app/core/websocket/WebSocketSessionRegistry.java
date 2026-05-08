package com.va.v.v_app.core.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory WebSocket session registry.
 *
 * Maps userId ↔ set of STOMP sessionIds.
 * A user may have multiple tabs/devices → multiple sessions.
 *
 * ─── PHASE-2 NOTE ───
 * Replace this with Redis-backed session registry when horizontal
 * scaling is needed. The interface stays the same.
 */
@Slf4j
@Component
public class WebSocketSessionRegistry {

    /** userId → set of STOMP sessionIds */
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    /** sessionId → userId (reverse lookup) */
    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();

    /** userId → lastSeenAt (updated on disconnect) */
    private final Map<String, LocalDateTime> lastSeenMap = new ConcurrentHashMap<>();

    /**
     * Register a WebSocket session for a user.
     */
    public void registerSession(String userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionToUser.put(sessionId, userId);
        log.info("WS session registered: userId={}, sessionId={}, totalSessions={}",
                userId, sessionId, userSessions.get(userId).size());
    }

    /**
     * Remove a WebSocket session. Returns the userId that was associated.
     */
    public String removeSession(String sessionId) {
        String userId = sessionToUser.remove(sessionId);
        if (userId != null) {
            Set<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                    lastSeenMap.put(userId, LocalDateTime.now());
                    log.info("WS all sessions closed for userId={}", userId);
                }
            }
        }
        return userId;
    }

    /**
     * Check if a user has at least one active WebSocket session.
     */
    public boolean isUserOnline(String userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Get all active session IDs for a user.
     */
    public Set<String> getSessionIds(String userId) {
        return userSessions.getOrDefault(userId, Collections.emptySet());
    }

    /**
     * Get the user ID associated with a session.
     */
    public String getUserId(String sessionId) {
        return sessionToUser.get(sessionId);
    }

    /**
     * Get last seen time for a user (when they went offline).
     */
    public LocalDateTime getLastSeen(String userId) {
        return lastSeenMap.get(userId);
    }

    /**
     * Get all currently online user IDs.
     */
    public Set<String> getOnlineUserIds() {
        return Collections.unmodifiableSet(userSessions.keySet());
    }

    /**
     * Get count of online users.
     */
    public int getOnlineCount() {
        return userSessions.size();
    }
}
