package com.example.user.api;

import java.util.UUID;

public final class UserResponses {
  private UserResponses() {}

  public record Registration(UUID userId, String username, String role) {}

  public record Login(String accessToken, String tokenType, long expiresIn) {}
}
