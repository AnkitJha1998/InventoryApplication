package com.example.notification.api;

import com.example.notification.persistence.entity.BackInStockRequest;
import com.example.notification.service.NotificationException;
import com.example.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @PostMapping("/back-in-stock")
  public ResponseEntity<BackInStockResponse> requestBackInStock(
      @Valid @RequestBody BackInStockRequestBody body,
      Authentication authentication,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization)
      throws NotificationException {
    BackInStockRequest request =
        notificationService.requestBackInStock(customerId(authentication), body, authorization);
    return ResponseEntity.status(HttpStatus.CREATED).body(BackInStockResponse.from(request));
  }

  @GetMapping("/count")
  public NotificationCountResponse unreadCount(Authentication authentication) {
    return new NotificationCountResponse(
        notificationService.unreadCount(customerId(authentication)));
  }

  @GetMapping
  public NotificationPageResponse notifications(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      Authentication authentication)
      throws NotificationException {
    return NotificationPageResponse.from(
        notificationService.findNotifications(customerId(authentication), page, size));
  }

  @PatchMapping("/read")
  public MarkNotificationsReadResponse markAsRead(
      @Valid @RequestBody MarkNotificationsReadBody body, Authentication authentication)
      throws NotificationException {
    int updatedCount =
        notificationService.markAsRead(customerId(authentication), body.notificationIds());
    return new MarkNotificationsReadResponse(updatedCount);
  }

  private UUID customerId(Authentication authentication) {
    return UUID.fromString(authentication.getName());
  }
}
