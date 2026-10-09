package com.example.user.api;

import com.example.user.service.UserExceptions;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  record ErrorResponse(Instant timestamp, int status, String error, String path) {}

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    UserExceptions.InvalidRoleException.class
  })
  ResponseEntity<ErrorResponse> badRequest(Exception exception, HttpServletRequest request) {
    return response(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  @ExceptionHandler(UserExceptions.DuplicateUsernameException.class)
  ResponseEntity<ErrorResponse> duplicate(Exception exception, HttpServletRequest request) {
    return response(HttpStatus.CONFLICT, exception.getMessage(), request);
  }

  @ExceptionHandler(UserExceptions.InvalidCredentialsException.class)
  ResponseEntity<ErrorResponse> credentials(Exception exception, HttpServletRequest request) {
    return response(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
  }

  private ResponseEntity<ErrorResponse> response(
      HttpStatus status, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(new ErrorResponse(Instant.now(), status.value(), message, request.getRequestURI()));
  }
}
