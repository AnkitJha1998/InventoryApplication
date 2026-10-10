package com.example.user.service;

import com.example.user.api.UserRequests;
import com.example.user.api.UserResponses;
import com.example.user.persistence.repository.UserRepository;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {
  private final UserRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthServiceImpl(
      UserRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Override
  public UserResponses.Login login(UserRequests.Login request)
      throws UserExceptions.InvalidCredentialsException {
    log.info("[login][{}] service entry username={}", Instant.now(),
        request == null ? "unknown" : request.username());
    var user = repository.findByUsername(request.username());
    if (user.isEmpty()) {
      log.info("[login][{}] login rejected username={} outcome=invalid-credentials",
          Instant.now(), request.username());
      throw new UserExceptions.InvalidCredentialsException();
    }
    if (!passwordEncoder.matches(request.password(), user.get().getPasswordHash())) {
      log.info("[login][{}] login rejected username={} outcome=invalid-credentials",
          Instant.now(), request.username());
      throw new UserExceptions.InvalidCredentialsException();
    }
    log.info("[login][{}] login succeeded userId={} outcome=authenticated",
        Instant.now(), user.get().getUserId());
    return new UserResponses.Login(
        jwtService.issue(user.get().getUserId(), user.get().getRole()), "Bearer", jwtService.expiresIn());
  }
}
