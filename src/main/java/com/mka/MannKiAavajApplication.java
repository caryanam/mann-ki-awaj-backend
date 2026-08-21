package com.mka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@org.springframework.scheduling.annotation.EnableScheduling
public class MannKiAavajApplication {

    public static void main(String[] args) {
        org.springframework.context.ConfigurableApplicationContext context = SpringApplication.run(MannKiAavajApplication.class, args);
        String port = context.getEnvironment().getProperty("server.port", "8080");

        System.out.printf("""
              ====================================================
                MANN KI AAVAJ BACKEND STARTED SUCCESSFULLY
              ====================================================

             Application : Mann Ki Aavaj (Voice of the Heart)
             Server      : http://localhost:%s
             Swagger UI  : http://localhost:%s/swagger-ui/index.html
             OpenAPI Doc : http://localhost:%s/v3/api-docs

              ====================================================
             %n""", port, port, port);
    }
}