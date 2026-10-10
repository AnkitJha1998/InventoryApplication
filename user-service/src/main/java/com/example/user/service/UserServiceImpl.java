package com.example.user.service;

import com.example.user.api.UserRequests;
import com.example.user.api.UserResponses;
import com.example.user.persistence.entity.User;
import com.example.user.persistence.repository.UserRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
  private final UserRepository repository;
  private final PasswordEncoder passwordEncoder;

  public UserServiceImpl(UserRepository repository, PasswordEncoder passwordEncoder) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional(
      rollbackFor = {
        UserExceptions.InvalidRoleException.class,
        UserExceptions.DuplicateUsernameException.class
      })
  public UserResponses.Registration register(UserRequests.Registration request)
      throws UserExceptions.InvalidRoleException, UserExceptions.DuplicateUsernameException {
    log.info("[register][{}] service entry username={}", Instant.now(),
        request == null ? "unknown" : request.username());
    String role = request.role().trim().toUpperCase(Locale.ROOT);
    if (!role.equals("CUSTOMER") && !role.equals("STORE_MANAGER")) {
      log.info("[register][{}] registration rejected username={} reason=invalid-role",
          Instant.now(), request.username());
      throw new UserExceptions.InvalidRoleException();
    }
    if (repository.existsByUsername(request.username())) {
      log.info("[register][{}] registration rejected username={} reason=duplicate-username",
          Instant.now(), request.username());
      throw new UserExceptions.DuplicateUsernameException();
    }
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
      log.error("[register][{}] registration persistence failed username={}",
          Instant.now(), request.username(), e);
      throw new UserExceptions.DuplicateUsernameException();
    }
    log.info("[register][{}] user registered userId={} username={} role={}",
        Instant.now(), id, request.username(), role);
    return new UserResponses.Registration(id, request.username(), role);
  }
}
