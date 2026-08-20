package com.mka.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.mka.entity.Profile;
import com.mka.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class PresenceManager {

    private final ProfileRepository profileRepository;
    private final SocketIOServer socketIOServer;

    // Maps userId -> Set of SocketIOClient session UUIDs (multi-session / multi-tab support)
    private final Map<Long, Set<UUID>> activeSessions = new ConcurrentHashMap<>();

    // Maps userId -> Timestamp (ms) of last heartbeat received
    private final Map<Long, Long> lastHeartbeats = new ConcurrentHashMap<>();

    // Maps userId -> Username string handle
    private final Map<Long, String> userNames = new ConcurrentHashMap<>();

    // Timeout threshold: 60 seconds without heartbeat marks user OFFLINE
    private static final long PRESENCE_TIMEOUT_MS = 60_000L;

    public void registerSession(Long userId, String username, UUID sessionId) {
        if (userId == null) return;

        if (username != null && !username.isBlank()) {
            userNames.put(userId, username);
        }

        activeSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        lastHeartbeats.put(userId, System.currentTimeMillis());

        Set<UUID> sessions = activeSessions.get(userId);
        if (sessions != null && sessions.size() == 1) {
            log.info("User {} ({}) status changed to ONLINE", userId, username);
            broadcastPresenceChange(userId, username, "ONLINE", null);
        }
    }

    public void touchHeartbeat(Long userId) {
        if (userId == null) return;
        lastHeartbeats.put(userId, System.currentTimeMillis());
    }

    public void unregisterSession(Long userId, UUID sessionId) {
        if (userId == null) return;

        Set<UUID> sessions = activeSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                activeSessions.remove(userId);
                lastHeartbeats.remove(userId);

                LocalDateTime now = LocalDateTime.now();
                persistLastSeen(userId, now);

                String username = userNames.getOrDefault(userId, "user_" + userId);
                log.info("User {} ({}) status changed to OFFLINE", userId, username);
                broadcastPresenceChange(userId, username, "OFFLINE", now);
            }
        }
    }

    public boolean isUserOnline(Long userId) {
        if (userId == null) return false;
        Set<UUID> sessions = activeSessions.get(userId);
        Long lastHb = lastHeartbeats.get(userId);
        if (sessions != null && !sessions.isEmpty() && lastHb != null) {
            return (System.currentTimeMillis() - lastHb) <= PRESENCE_TIMEOUT_MS;
        }
        return false;
    }

    public LocalDateTime getLastSeen(Long userId) {
        if (userId == null) return null;
        return profileRepository.findByUserId(userId)
                .map(Profile::getLastSeen)
                .orElse(null);
    }

    @Scheduled(fixedRate = 15000)
    public void checkPresenceTimeouts() {
        long nowMs = System.currentTimeMillis();
        for (Map.Entry<Long, Long> entry : lastHeartbeats.entrySet()) {
            Long userId = entry.getKey();
            Long lastHb = entry.getValue();
            if (lastHb != null && (nowMs - lastHb) > PRESENCE_TIMEOUT_MS) {
                activeSessions.remove(userId);
                lastHeartbeats.remove(userId);

                LocalDateTime now = LocalDateTime.now();
                persistLastSeen(userId, now);

                String username = userNames.getOrDefault(userId, "user_" + userId);
                log.info("User {} ({}) timed out -> OFFLINE", userId, username);
                broadcastPresenceChange(userId, username, "OFFLINE", now);
            }
        }
    }

    private void persistLastSeen(Long userId, LocalDateTime lastSeenTime) {
        try {
            profileRepository.findByUserId(userId).ifPresent(profile -> {
                profile.setLastSeen(lastSeenTime);
                profileRepository.save(profile);
            });
        } catch (Exception e) {
            log.error("Failed to persist lastSeen for user {}: {}", userId, e.getMessage());
        }
    }

    public void broadcastPresenceChange(Long userId, String username, String status, LocalDateTime lastSeen) {
        if (socketIOServer == null || userId == null) return;

        String handle = userNames.getOrDefault(userId, username);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("username", handle);
        payload.put("userHandle", handle);
        payload.put("status", status);
        payload.put("isOnline", "ONLINE".equalsIgnoreCase(status));
        payload.put("lastSeen", lastSeen != null ? lastSeen.toString() : null);

        try {
            socketIOServer.getBroadcastOperations().sendEvent("user_presence_changed", payload);
        } catch (Exception e) {
            log.error("Failed to broadcast presence change: {}", e.getMessage());
        }
    }
}
