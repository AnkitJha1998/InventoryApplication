package com.example.notification.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
  @Id
  @Column(name = "notification_id", nullable = false)
  private UUID notificationId;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "product_id", nullable = false)
  private String productId;

  @Column(name = "request_id", nullable = false)
  private UUID requestId;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(nullable = false, length = 512)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private NotificationStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "read_at")
  private Instant readAt;
}
