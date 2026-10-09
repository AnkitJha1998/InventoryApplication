package com.example.notification.security;

public class JwtValidationException extends Exception {
  public JwtValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
