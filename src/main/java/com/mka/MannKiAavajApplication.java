package com.mka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MannKiAavajApplication {

    public static void main(String[] args) {
        SpringApplication.run(MannKiAavajApplication.class, args);

        System.out.println("""
              ====================================================
                MANN KI AAVAJ BACKEND STARTED SUCCESSFULLY
              ====================================================

             Application : Mann Ki Aavaj (Voice of the Heart)
             Server      : http://localhost:8080
             Swagger UI  : http://localhost:8080/swagger-ui/index.html
             OpenAPI Doc : http://localhost:8080/v3/api-docs

             ====================================================
             """);
    }
}
