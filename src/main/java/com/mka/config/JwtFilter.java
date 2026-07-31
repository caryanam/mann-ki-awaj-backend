/*package com.tours.config;



import jakarta.servlet.*;

import jakarta.servlet.http.*;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;



import java.io.IOException;



@Component

@RequiredArgsConstructor

public class JwtFilter extends OncePerRequestFilter {



    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;



    @Override

    protected void doFilterInternal(

            HttpServletRequest request,

            HttpServletResponse response,

            FilterChain filterChain)

            throws ServletException, IOException {



        String path = request.getServletPath();



        if (path.startsWith("/swagger-ui")

                || path.startsWith("/v3/api-docs")

                || path.startsWith("/webjars")

                || path.equals("/swagger-ui.html")

                || path.startsWith("/api/auth")) {



            filterChain.doFilter(request, response);

            return;

        }



        String header = request.getHeader("Authorization");



        if (header == null || !header.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);

            return;

        }



        String token = header.substring(7);



        String username = jwtService.extractUsername(token);



        if (username != null &&

                SecurityContextHolder.getContext().getAuthentication() == null) {



            UserDetails user =

                    userDetailsService.loadUserByUsername(username);
            System.out.println("Username : " + username);
            System.out.println("Authorities : " + user.getAuthorities());


            if (jwtService.validateToken(token, user)) {



                UsernamePasswordAuthenticationToken auth =

                        new UsernamePasswordAuthenticationToken(

                                user,

                                null,

                                user.getAuthorities());



                auth.setDetails(

                        new WebAuthenticationDetailsSource()

                                .buildDetails(request));



                SecurityContextHolder.getContext()

                        .setAuthentication(auth);

            }

        }



        filterChain.doFilter(request, response);

    }

}

 */
package com.mka.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/api/auth")) {

            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            String token = header.substring(7);

            String username = jwtService.extractUsername(token);

            if (username != null
                    && SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                UserDetails user =
                        userDetailsService.loadUserByUsername(username);

               /* System.out.println("Username: " + username);
                System.out.println(
                        "Authorities: " + user.getAuthorities()
                ); */

                if (jwtService.validateToken(token, user)) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    user.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }
            }

        } catch (Exception exception) {

            SecurityContextHolder.clearContext();

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType("application/json");

            response.getWriter().write("""
                    {
                      "success": false,
                      "status": 401,
                      "message": "Invalid or expired token. Please login again."
                    }
                    """);

            return;
        }

        filterChain.doFilter(request, response);
    }
}
