package com.mka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class  MannKiAavajApplication {

    public static void main(String[] args) {
        SpringApplication.run(MannKiAavajApplication.class, args);

        System.out.println("""
              ====================================================
                MANN KI AAVAJ BACKEND STARTED
              ====================================================

             Application : Mann Ki Aavaj
             Server      : http://localhost:8080
             Swagger UI  : http://localhost:8080/swagger-ui/index.html
             Swagger UI  : http://localhost:8080/swagger-ui/index.html

             ====================================================
             """);

    }

}
