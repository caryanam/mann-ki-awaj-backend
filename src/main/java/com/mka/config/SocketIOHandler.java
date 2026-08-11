package com.mka.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SocketIOHandler {

    public SocketIOHandler(SocketIOServer server) {
        server.addConnectListener(client -> {
            log.info("Client connected to Socket.IO: {}", client.getSessionId());
        });

        server.addDisconnectListener(client -> {
            log.info("Client disconnected from Socket.IO: {}", client.getSessionId());
        });

        server.addEventListener("join_room", String.class, (client, roomId, ackSender) -> {
            client.joinRoom("room_" + roomId);
            log.info("Client {} joined room: room_{}", client.getSessionId(), roomId);
        });

        server.addEventListener("leave_room", String.class, (client, roomId, ackSender) -> {
            client.leaveRoom("room_" + roomId);
            log.info("Client {} left room: room_{}", client.getSessionId(), roomId);
        });

        server.addEventListener("join_user_room", String.class, (client, userId, ackSender) -> {
            if (userId != null && !userId.isBlank()) {
                client.joinRoom("user_" + userId);
                log.info("Client {} joined personal user room: user_{}", client.getSessionId(), userId);
            }
        });
    }
}
