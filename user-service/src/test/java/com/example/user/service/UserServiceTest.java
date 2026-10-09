package com.example.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.user.api.UserRequests;
import com.example.user.persistence.User;
import com.example.user.persistence.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UserServiceTest {
  @Test
  void registrationGeneratesUuidAndStoresBcryptHash() {
    UserRepository repository = mock(UserRepository.class);
    var encoder = new BCryptPasswordEncoder();
    when(repository.existsByUsername("alice")).thenReturn(false);
    when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        new UserServiceImpl(repository, encoder)
            .register(new UserRequests.Registration("alice", "password", "CUSTOMER"));

    assertNotNull(response.userId());
    assertEquals("CUSTOMER", response.role());
    var saved = org.mockito.ArgumentCaptor.forClass(User.class);
    verify(repository).save(saved.capture());
    assertNotEquals("password", saved.getValue().getPasswordHash());
    assertTrue(encoder.matches("password", saved.getValue().getPasswordHash()));
    assertEquals(response.userId(), saved.getValue().getUserId());
  }

  @Test
  void duplicateUsernameIsRejected() {
    var repository = mock(UserRepository.class);
    when(repository.existsByUsername("alice")).thenReturn(true);
    assertThrows(
        UserExceptions.DuplicateUsernameException.class,
        () ->
            new UserServiceImpl(repository, new BCryptPasswordEncoder())
                .register(new UserRequests.Registration("alice", "password", "CUSTOMER")));
  }

  @Test
  void loginIssuesBearerTokenForCorrectPassword() {
    var repository = mock(UserRepository.class);
    var encoder = new BCryptPasswordEncoder();
    var id = java.util.UUID.randomUUID();
    when(repository.findByUsername("alice"))
        .thenReturn(
            Optional.of(
                User.builder()
                    .userId(id)
                    .username("alice")
                    .passwordHash(encoder.encode("correct"))
                    .role("CUSTOMER")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build()));
    var jwt = mock(JwtService.class);
    when(jwt.issue(id, "CUSTOMER")).thenReturn("signed-token");
    when(jwt.expiresIn()).thenReturn(3600L);

    var response =
        new AuthServiceImpl(repository, encoder, jwt)
            .login(new UserRequests.Login("alice", "correct"));
    assertEquals("signed-token", response.accessToken());
    assertEquals("Bearer", response.tokenType());
    assertEquals(3600, response.expiresIn());
  }

  @Test
  void loginRejectsBadPassword() {
    var repository = mock(UserRepository.class);
    var encoder = new BCryptPasswordEncoder();
    when(repository.findByUsername("alice"))
        .thenReturn(
            Optional.of(
                User.builder()
                    .userId(java.util.UUID.randomUUID())
                    .username("alice")
                    .passwordHash(encoder.encode("correct"))
                    .role("CUSTOMER")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build()));
    assertThrows(
        UserExceptions.InvalidCredentialsException.class,
        () ->
            new AuthServiceImpl(repository, encoder, mock(JwtService.class))
                .login(new UserRequests.Login("alice", "wrong")));
  }
}
