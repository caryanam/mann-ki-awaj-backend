package com.mka.config;
 
import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;
 
@Component
@RequiredArgsConstructor
public class SocketIOServerRunner implements CommandLineRunner {
 
    private final SocketIOServer server;
 
    @Override
    public void run(String... args) {
        try {
            System.out.println("Starting Socket.IO Server...");
            server.start();
            System.out.println("Socket.IO Server started successfully.");
        } catch (Exception e) {
            System.err.println("Failed to start Socket.IO Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
 
    @PreDestroy
    public void stopServer() {
        System.out.println("Stopping Socket.IO Server...");
        server.stop();
    }
}