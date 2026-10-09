package com.example.user.api;

import jakarta.validation.constraints.NotBlank;

public final class UserRequests {
  private UserRequests() {}

  public record Registration(
      @NotBlank String username, @NotBlank String password, @NotBlank String role) {}

  public record Login(@NotBlank String username, @NotBlank String password) {}
}
