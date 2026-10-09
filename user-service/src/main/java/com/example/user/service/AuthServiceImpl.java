package com.example.user.service;

import com.example.user.api.UserRequests;
import com.example.user.api.UserResponses;
import com.example.user.persistence.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
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
  public UserResponses.Login login(UserRequests.Login request) {
    var user =
        repository
            .findByUsername(request.username())
            .orElseThrow(UserExceptions.InvalidCredentialsException::new);
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new UserExceptions.InvalidCredentialsException();
    }
    return new UserResponses.Login(
        jwtService.issue(user.getUserId(), user.getRole()), "Bearer", jwtService.expiresIn());
  }
}
