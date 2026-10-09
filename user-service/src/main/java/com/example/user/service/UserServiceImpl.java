package com.example.user.service;

import com.example.user.api.UserRequests;
import com.example.user.api.UserResponses;
import com.example.user.persistence.User;
import com.example.user.persistence.UserRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
  private final UserRepository repository;
  private final PasswordEncoder passwordEncoder;

  public UserServiceImpl(UserRepository repository, PasswordEncoder passwordEncoder) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public UserResponses.Registration register(UserRequests.Registration request) {
    String role = request.role().trim().toUpperCase(Locale.ROOT);
    if (!role.equals("CUSTOMER") && !role.equals("STORE_MANAGER")) {
      throw new UserExceptions.InvalidRoleException();
    }
    if (repository.existsByUsername(request.username()))
      throw new UserExceptions.DuplicateUsernameException();
    UUID id = UUID.randomUUID();
    try {
      repository.save(
          User.builder()
              .userId(id)
              .username(request.username())
              .passwordHash(passwordEncoder.encode(request.password()))
              .role(role)
              .createdAt(Instant.now())
              .updatedAt(Instant.now())
              .build());
    } catch (DataIntegrityViolationException e) {
      throw new UserExceptions.DuplicateUsernameException();
    }
    return new UserResponses.Registration(id, request.username(), role);
  }
}
