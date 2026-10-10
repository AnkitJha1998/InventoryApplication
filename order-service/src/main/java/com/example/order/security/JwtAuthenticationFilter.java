package com.example.order.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String authorization = request.getHeader("Authorization");
    if (authorization != null && authorization.startsWith("Bearer ")) {
      try {
        JwtService.Claims claims = jwtService.validate(authorization.substring(7));
        var authentication =
            new UsernamePasswordAuthenticationToken(
                claims.userId().toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + claims.role())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info(
            "[doFilterInternal][{}] JWT authentication accepted userId={}",
            Instant.now(),
            claims.userId());
      } catch (JwtValidationException exception) {
        log.error("[doFilterInternal][{}] JWT authentication rejected", Instant.now(), exception);
        // Leave the context anonymous; the security chain returns 401 for protected routes.
      }
    }
    chain.doFilter(request, response);
  }
}
