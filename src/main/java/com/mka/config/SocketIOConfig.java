package com.mka.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.AuthorizationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class SocketIOConfig {

    @Value("${socket.io.port:8085}")
    private int socketIoPort;

    @Value("${socket.io.host:0.0.0.0}")
    private String socketIoHost;

    @Value("${socket.io.allowed-origin:*}")
    private String allowedOrigin;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(socketIoHost);
        config.setPort(socketIoPort);

        if (allowedOrigin == null || allowedOrigin.trim().isEmpty() || "*".equals(allowedOrigin.trim())) {
            config.setOrigin(allowedOrigin);
        } else {
            List<String> allowedOrigins = Arrays.stream(allowedOrigin.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            // Setting origin to null causes netty-socketio to echo back the client's request origin
            config.setOrigin(null);

            // Authorize only handshakes whose Origin matches the explicit allowed origins
            config.setAuthorizationListener(data -> {
                String reqOrigin = data.getHttpHeaders().get("Origin");
                if (reqOrigin == null || reqOrigin.isBlank()) {
                    reqOrigin = data.getHttpHeaders().get("origin");
                }
                if (reqOrigin == null || reqOrigin.isBlank()) {
                    return new AuthorizationResult(true);
                }
                boolean allowed = allowedOrigins.contains(reqOrigin.trim());
                return new AuthorizationResult(allowed);
            });
        }

        config.setTransports(Transport.POLLING, Transport.WEBSOCKET);
        config.setAllowCustomRequests(true);
        return new SocketIOServer(config);
    }
}