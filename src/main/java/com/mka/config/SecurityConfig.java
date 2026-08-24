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

                        // Authentication & Public APIs
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/v1/translation/**",
                                "/api/mood/**"
                        ).permitAll()

                        // Public, read-only published music catalog
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/music/tracks",
                                "/api/music/tracks/*"
                        ).permitAll()

                        // Static uploaded files and published music media
                        .requestMatchers("/uploads/**", "/media/music/**").permitAll()

                        // Public feed, posts, comments, profiles & reasons view
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/posts",
                                "/api/posts/*",
                                "/api/posts/**",
                                "/api/posts/*/comments",
                                "/api/profile/*",
                                "/api/users/*",
                                "/api/report-reasons",
                                "/api/mood/india",
                                "/api/topics",
                                "/api/topics/*",
                                "/api/topics/*/comments"
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
                            String origin = request.getHeader("Origin");
                            if (origin != null && !origin.isBlank()) {
                                response.setHeader("Access-Control-Allow-Origin", origin);
                                response.setHeader("Access-Control-Allow-Credentials", "true");
                                response.setHeader("Access-Control-Allow-Headers", "*");
                                response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
                            }

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
                            String origin = request.getHeader("Origin");
                            if (origin != null && !origin.isBlank()) {
                                response.setHeader("Access-Control-Allow-Origin", origin);
                                response.setHeader("Access-Control-Allow-Credentials", "true");
                                response.setHeader("Access-Control-Allow-Headers", "*");
                                response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
                            }

                            response.getWriter().write("""
                                    {
                                      "success": false,
                                      "status": 403,
                                      "message": "Access denied. You are not authorized to access this resource."
                                    }
                                    """);
                        })
                )

                .addFilterBefore(corsFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.filter.CorsFilter corsFilter() {
        return new org.springframework.web.filter.CorsFilter(corsConfigurationSource());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins for production and local environments
        configuration.setAllowedOrigins(List.of(
                "https://awaazmanki.com",
                "https://www.awaazmanki.com",
                "https://api.awaazmanki.com",
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:8080",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000"
        ));

        // Allowed origin patterns for all subdomains & ports
        configuration.setAllowedOriginPatterns(List.of(
                "https://*.awaazmanki.com",
                "http://localhost:*",
                "http://127.0.0.1:*"
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
                 "Authorization",
                 "Content-Type",
                 "Content-Length",
                 "Accept-Ranges",
                 "Content-Range",
                 "Access-Control-Allow-Origin",
                 "Access-Control-Allow-Credentials"
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
