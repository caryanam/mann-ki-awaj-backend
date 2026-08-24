package com.mka.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "https://awaazmanki.com",
                        "https://www.awaazmanki.com",
                        "https://api.awaazmanki.com",
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "http://127.0.0.1:5173",
                        "http://127.0.0.1:3000"
                )
                .allowedOriginPatterns(
                        "https://*.awaazmanki.com",
                        "http://localhost:*",
                        "http://127.0.0.1:*"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type", "Content-Length", "Accept-Ranges", "Content-Range", "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File uploadsDir = new File("uploads");
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs();
        }

        String absolutePath = uploadsDir.getAbsolutePath();
        if (!absolutePath.endsWith(File.separator)) {
            absolutePath += File.separator;
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                        "file:" + absolutePath,
                        "file:uploads/",
                        "file:./uploads/"
                );
    }
}

