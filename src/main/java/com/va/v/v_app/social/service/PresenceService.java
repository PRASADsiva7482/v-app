package com.va.v.v_app.social.service;

import com.va.v.v_app.core.websocket.WebSocketSessionRegistry;
import com.va.v.v_app.social.dto.response.PresenceEvent;
import com.va.v.v_app.social.model.UserPresence;
import com.va.v.v_app.social.repository.UserPresenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Manages user online/offline presence.
 *
 * Primary source of truth: in-memory WebSocketSessionRegistry
 * DB (user_presence): backup, used for last_seen_at persistence
 *
 * On connect → mark ONLINE, broadcast to contacts
 * On disconnect → mark OFFLINE, set last_seen, broadcast to contacts
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final WebSocketSessionRegistry sessionRegistry;
    private final UserPresenceRepository presenceRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Called when a user's WebSocket connects.
     */
    @Transactional
    public void userConnected(String userId) {
        // Update DB presence
        UserPresence presence = presenceRepository.findById(userId)
                .orElse(UserPresence.builder().userId(userId).build());
        presence.setIsOnline(true);
        presenceRepository.save(presence);

        // Broadcast online status
        broadcastPresence(userId, true, null);
        log.info("User ONLINE: {}", userId);
    }

    /**
     * Called when ALL WebSocket sessions for a user are closed.
     */
    @Transactional
    public void userDisconnected(String userId) {
        LocalDateTime lastSeen = LocalDateTime.now();

        // Update DB presence
        UserPresence presence = presenceRepository.findById(userId)
                .orElse(UserPresence.builder().userId(userId).build());
        presence.setIsOnline(false);
        presence.setLastSeenAt(lastSeen);
        presenceRepository.save(presence);

        // Broadcast offline status
        broadcastPresence(userId, false, lastSeen);
        log.info("User OFFLINE: {}, lastSeen={}", userId, lastSeen);
    }

    /**
     * Check if a user is currently online (in-memory check).
     */
    public boolean isOnline(String userId) {
        return sessionRegistry.isUserOnline(userId);
    }

    /**
     * Get last seen time for a user.
     */
    public LocalDateTime getLastSeen(String userId) {
        // Check in-memory first
        LocalDateTime inMemory = sessionRegistry.getLastSeen(userId);
        if (inMemory != null) {
            return inMemory;
        }
        // Fallback to DB
        return presenceRepository.findById(userId)
                .map(UserPresence::getLastSeenAt)
                .orElse(null);
    }

    /**
     * Broadcast presence event to all connected users.
     * In Phase-2, this would publish to a broker topic instead.
     */
    private void broadcastPresence(String userId, boolean online, LocalDateTime lastSeen) {
        PresenceEvent event = PresenceEvent.builder()
                .userId(userId)
                .online(online)
                .lastSeenAt(lastSeen)
                .build();

        // Broadcast to a global presence topic
        messagingTemplate.convertAndSend("/topic/presence", event);
    }
}
