package com.example.user.api;

import com.example.user.service.AuthService;
import com.example.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {
  private final UserService userService;
  private final AuthService authService;

  public UserController(UserService userService, AuthService authService) {
    this.userService = userService;
    this.authService = authService;
  }

  @PostMapping("/users")
  public ResponseEntity<UserResponses.Registration> register(
      @Valid @RequestBody UserRequests.Registration request) {
    return ResponseEntity.status(201).body(userService.register(request));
  }

  @PostMapping("/auth/login")
  public ResponseEntity<UserResponses.Login> login(@Valid @RequestBody UserRequests.Login request) {
    return ResponseEntity.ok(authService.login(request));
  }
}
