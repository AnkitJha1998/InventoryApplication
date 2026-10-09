package com.example.inventory.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService)
            throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers("/actuator/health")
                                .permitAll()
                                .requestMatchers("/inventory/create")
                                .hasRole("STORE_MANAGER")
                                .requestMatchers("/inventory/reduce")
                                .hasAnyRole("CUSTOMER", "STORE_MANAGER")
                                .requestMatchers("/inventory/increase")
                                .hasRole("STORE_MANAGER")
                                .requestMatchers(HttpMethod.GET, "/inventory", "/inventory/**")
                                .hasAnyRole("CUSTOMER", "STORE_MANAGER")
                                .anyRequest()
                                .authenticated())
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(
                        exceptions -> exceptions
                                .authenticationEntryPoint(
                                        (request, response, exception) ->
                                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                                .accessDeniedHandler(
                                        (request, response, exception) ->
                                                response.sendError(HttpServletResponse.SC_FORBIDDEN)))
                .build();
    }
}
