package com.mka.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // Authentication APIs (register, login, verify, forgot-password, reset-password)
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/v1/translation/**"
                        ).permitAll()

                        // Static uploaded files
                        .requestMatchers("/uploads/**").permitAll()

                        // Public feed, posts, comments, profiles & reasons view
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/posts",
                                "/api/posts/*",
                                "/api/posts/*/comments",
                                "/api/profile/*",
                                "/api/users/*",
                                "/api/report-reasons"
                        ).permitAll()

                        // Admin APIs
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Swagger APIs
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()

                        // CORS Preflight
                        .requestMatchers(
                                org.springframework.http.HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Every other API requires authentication
                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint((request, response, ex) -> {

                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");

                            response.getWriter().write("""
                                    {
                                      "success": false,
                                      "status": 401,
                                      "message": "Unauthorized. Please provide a valid Bearer token."
                                    }
                                    """);
                        })

                        .accessDeniedHandler((request, response, ex) -> {

                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");

                            response.getWriter().write("""
                                    {
                                      "success": false,
                                      "status": 403,
                                      "message": "Access denied. You are not authorized to access this resource."
                                    }
                                    """);
                        })
                )

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }



    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins for local development and production environments
        configuration.setAllowedOrigins(List.of(
                "https://awaazmanki.com",
                "https://www.awaazmanki.com",
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setExposedHeaders(List.of(
                "Authorization"
        ));

        // Required for authenticated cross-origin requests
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}