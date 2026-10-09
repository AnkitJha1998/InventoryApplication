package com.example.order.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

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
      } catch (JwtValidationException ignored) {
        // Leave the context anonymous; the security chain returns 401 for protected routes.
      }
    }
    chain.doFilter(request, response);
  }
}
