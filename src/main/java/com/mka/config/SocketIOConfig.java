package com.mka.config;
 
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
 
@Configuration
public class SocketIOConfig {
 
    @Value("${socket.io.port:8090}")
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
        config.setOrigin(allowedOrigin);
        config.setTransports(Transport.POLLING, Transport.WEBSOCKET);
        config.setAllowCustomRequests(true);
        return new SocketIOServer(config);
    }
}