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
    public void run(String... args) throws Exception {
        System.out.println("Starting Socket.IO Server on port 8085...");
        server.start();
    }

    @PreDestroy
    public void stopServer() {
        System.out.println("Stopping Socket.IO Server...");
        server.stop();
    }
}
