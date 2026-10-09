package com.example.notification.api;

import com.example.notification.service.DownstreamQueryException;
import com.example.notification.service.NotificationException;
import com.example.notification.service.ProductNotFoundException;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class, ProductNotFoundException.class,
            NotificationException.class})
    public ResponseEntity<Map<String, String>> badRequest(Exception exception) {
        String message = exception instanceof MethodArgumentNotValidException validation
                ? validation.getBindingResult().getFieldError() == null
                ? "invalid request"
                : validation.getBindingResult().getFieldError().getDefaultMessage()
                : exception.getMessage();
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler(DownstreamQueryException.class)
    public ResponseEntity<Map<String, String>> downstream(DownstreamQueryException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", exception.getMessage()));
    }
}
