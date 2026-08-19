package com.mka.config;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.mka.entity.ChatRoom;
import com.mka.entity.User;
import com.mka.repository.ChatRoomRepository;
import com.mka.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SocketIOHandler {

    private final PresenceManager presenceManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;

    public SocketIOHandler(
            SocketIOServer server,
            PresenceManager presenceManager,
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            UserRepository userRepository,
            ChatRoomRepository chatRoomRepository) {

        this.presenceManager = presenceManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.chatRoomRepository = chatRoomRepository;

        server.addConnectListener(client -> {
            String token = extractToken(client);
            if (token != null && !token.isBlank()) {
                try {
                    String username = jwtService.extractUsername(token);
                    if (username != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        if (jwtService.validateToken(token, userDetails)) {
                            User user = userRepository.findByEmail(username).orElse(null);
                            if (user != null && Boolean.TRUE.equals(user.getActive())) {
                                client.set("authenticatedUserId", user.getId());
                                client.set("authenticatedUserEmail", user.getEmail());
                                log.info("Socket client {} authenticated successfully for user ID {}", client.getSessionId(), user.getId());
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Socket connection authentication failed for client {}: {}", client.getSessionId(), e.getMessage());
                }
            }
            log.warn("Socket client {} connection rejected due to invalid/missing JWT or inactive account", client.getSessionId());
            client.disconnect();
        });

        server.addDisconnectListener(client -> {
            log.info("Client disconnected from Socket.IO: {}", client.getSessionId());
            Long authUserId = getAuthUserId(client);
            if (authUserId != null) {
                try {
                    presenceManager.unregisterSession(authUserId, client.getSessionId());
                } catch (Exception e) {
                    log.warn("Failed to unregister presence session on disconnect for user {}: {}", authUserId, e.getMessage());
                }
            }
        });

        server.addEventListener("join_room", String.class, (client, roomIdStr, ackSender) -> {
            Long authUserId = getAuthUserId(client);
            if (authUserId == null) {
                log.warn("Unauthenticated socket attempt to join_room from client {}", client.getSessionId());
                client.disconnect();
                return;
            }

            Long roomId;
            try {
                roomId = Long.parseLong(roomIdStr);
            } catch (Exception e) {
                log.warn("Invalid roomId format received in join_room: {}", roomIdStr);
                return;
            }

            ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
            if (room == null) {
                log.warn("User {} attempted to join non-existent room ID: {}", authUserId, roomId);
                return;
            }

            boolean isParticipant = (room.getParticipant1() != null && room.getParticipant1().getId().equals(authUserId))
                    || (room.getParticipant2() != null && room.getParticipant2().getId().equals(authUserId));

            if (!isParticipant) {
                log.warn("Security Alert: User {} attempted unauthorized join of chat room {}", authUserId, roomId);
                return;
            }

            client.joinRoom("room_" + roomId);
            log.info("Authorized user {} joined chat room: room_{}", authUserId, roomId);
        });

        server.addEventListener("leave_room", String.class, (client, roomId, ackSender) -> {
            client.leaveRoom("room_" + roomId);
            log.info("Client {} left room: room_{}", client.getSessionId(), roomId);
        });

        server.addEventListener("join_user_room", String.class, (client, userIdStr, ackSender) -> {
            Long authUserId = getAuthUserId(client);
            if (authUserId == null) {
                log.warn("Unauthenticated socket attempt to join_user_room from client {}", client.getSessionId());
                client.disconnect();
                return;
            }

            if (userIdStr == null || !String.valueOf(authUserId).equals(userIdStr.trim())) {
                log.warn("Security Alert: User {} attempted unauthorized join of user room user_{}", authUserId, userIdStr);
                return;
            }

            String roomName = "user_" + authUserId;
            client.joinRoom(roomName);
            client.set("userId", String.valueOf(authUserId));
            try {
                presenceManager.registerSession(authUserId, roomName, client.getSessionId());
            } catch (Exception e) {
                log.warn("Failed to register user presence room for user {}: {}", authUserId, e.getMessage());
            }
            log.info("Authorized user {} joined personal user room: {}", authUserId, roomName);
        });

        server.addEventListener("presence_heartbeat", Object.class, (client, data, ackSender) -> {
            Long authUserId = getAuthUserId(client);
            if (authUserId == null) {
                log.warn("Unauthenticated presence_heartbeat attempt from client {}", client.getSessionId());
                client.disconnect();
                return;
            }

            try {
                client.set("userId", String.valueOf(authUserId));
                presenceManager.touchHeartbeat(authUserId);
                presenceManager.registerSession(authUserId, "user_" + authUserId, client.getSessionId());
            } catch (Exception e) {
                log.warn("Error processing presence heartbeat for user {}: {}", authUserId, e.getMessage());
            }
        });
    }

    private Long getAuthUserId(SocketIOClient client) {
        if (client == null) return null;
        Object val = client.get("authenticatedUserId");
        if (val instanceof Long) return (Long) val;
        if (val instanceof String) {
            try { return Long.parseLong((String) val); } catch (Exception ignored) {}
        }
        return null;
    }

    private String extractToken(SocketIOClient client) {
        if (client == null || client.getHandshakeData() == null) return null;

        String token = client.getHandshakeData().getSingleUrlParam("token");
        if (token != null && !token.isBlank()) return token.trim();

        token = client.getHandshakeData().getSingleUrlParam("auth_token");
        if (token != null && !token.isBlank()) return token.trim();

        token = client.getHandshakeData().getSingleUrlParam("accessToken");
        if (token != null && !token.isBlank()) return token.trim();

        if (client.getHandshakeData().getHttpHeaders() != null) {
            String authHeader = client.getHandshakeData().getHttpHeaders().get("Authorization");
            if (authHeader == null || authHeader.isBlank()) {
                authHeader = client.getHandshakeData().getHttpHeaders().get("authorization");
            }
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7).trim();
            }
        }
        return null;
    }
}
