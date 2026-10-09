package com.example.order.api;

import com.example.order.service.InventoryDownstreamException;
import com.example.order.service.OrderBusinessException;
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
    OrderBusinessException.class,
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class
  })
  ResponseEntity<ErrorResponse> badRequest(Exception e, HttpServletRequest req) {
    return response(HttpStatus.BAD_REQUEST, e.getMessage(), req);
  }

  @ExceptionHandler(InventoryDownstreamException.class)
  ResponseEntity<ErrorResponse> downstream(InventoryDownstreamException e, HttpServletRequest req) {
    return response(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), req);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest req) {
    return response(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", req);
  }

  private ResponseEntity<ErrorResponse> response(
      HttpStatus status, String message, HttpServletRequest req) {
    return ResponseEntity.status(status)
        .body(new ErrorResponse(Instant.now(), status.value(), message, req.getRequestURI()));
  }
}
