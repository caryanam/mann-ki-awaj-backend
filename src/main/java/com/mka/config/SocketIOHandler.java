package com.mka.config;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class SocketIOHandler {

    private final PresenceManager presenceManager;

    public SocketIOHandler(SocketIOServer server, PresenceManager presenceManager) {
        this.presenceManager = presenceManager;

        server.addConnectListener(client -> {
            log.info("Client connected to Socket.IO: {}", client.getSessionId());
        });

        server.addDisconnectListener(client -> {
            log.info("Client disconnected from Socket.IO: {}", client.getSessionId());
            Object userIdObj = client.get("userId");
            if (userIdObj != null) {
                try {
                    Long userId = Long.parseLong(userIdObj.toString());
                    presenceManager.unregisterSession(userId, client.getSessionId());
                } catch (Exception e) {
                    log.warn("Failed to unregister presence session on disconnect: {}", e.getMessage());
                }
            }
        });

        server.addEventListener("join_room", String.class, (client, roomId, ackSender) -> {
            client.joinRoom("room_" + roomId);
            log.info("Client {} joined room: room_{}", client.getSessionId(), roomId);
        });

        server.addEventListener("leave_room", String.class, (client, roomId, ackSender) -> {
            client.leaveRoom("room_" + roomId);
            log.info("Client {} left room: room_{}", client.getSessionId(), roomId);
        });

        server.addEventListener("join_user_room", String.class, (client, userIdStr, ackSender) -> {
            if (userIdStr != null && !userIdStr.isBlank()) {
                client.joinRoom("user_" + userIdStr);
                try {
                    Long userId = Long.parseLong(userIdStr);
                    client.set("userId", userIdStr);
                    presenceManager.registerSession(userId, "user_" + userIdStr, client.getSessionId());
                } catch (Exception e) {
                    log.warn("Failed to register user presence room for {}: {}", userIdStr, e.getMessage());
                }
                log.info("Client {} joined personal user room: user_{}", client.getSessionId(), userIdStr);
            }
        });

        server.addEventListener("presence_heartbeat", Object.class, (client, data, ackSender) -> {
            try {
                Long userId = null;
                if (data instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) data;
                    Object uidObj = map.get("userId");
                    if (uidObj != null) {
                        userId = Long.parseLong(uidObj.toString());
                    }
                } else if (data instanceof String) {
                    userId = Long.parseLong((String) data);
                }

                if (userId != null) {
                    client.set("userId", String.valueOf(userId));
                    presenceManager.touchHeartbeat(userId);
                    presenceManager.registerSession(userId, "user_" + userId, client.getSessionId());
                }
            } catch (Exception e) {
                log.warn("Error processing presence heartbeat: {}", e.getMessage());
            }
        });
    }
}
