package com.example.notification.api;

import com.example.notification.persistence.entity.BackInStockRequest;
import com.example.notification.service.BackInStockService;
import com.example.notification.service.NotificationException;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
  private final BackInStockService backInStockService;

  public NotificationController(BackInStockService backInStockService) {
    this.backInStockService = backInStockService;
  }

  @PostMapping("/back-in-stock")
  public ResponseEntity<BackInStockResponse> requestBackInStock(
      @Valid @RequestBody BackInStockRequestBody body,
      Authentication authentication,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization)
      throws NotificationException {
    UUID customerId = UUID.fromString(authentication.getName());
    BackInStockRequest request = backInStockService.request(customerId, body, authorization);
    return ResponseEntity.status(HttpStatus.CREATED).body(BackInStockResponse.from(request));
  }
}
