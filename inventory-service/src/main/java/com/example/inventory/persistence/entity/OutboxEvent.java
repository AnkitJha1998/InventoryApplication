package com.example.inventory.persistence.entity;

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
@Table(name = "outbox_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
  @Id
  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "product_id", nullable = false)
  private String productId;

  @Column(name = "product_name")
  private String productName;

  @Column(name = "product_type")
  private String productType;

  @Column(name = "previous_count", nullable = false)
  private int previousCount;

  @Column(name = "current_count", nullable = false)
  private int currentCount;

  @Column(nullable = false, length = 64)
  private String topic;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private OutboxStatus status;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "lease_until")
  private Instant leaseUntil;

  @Column(name = "last_error", length = 1000)
  private String lastError;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  public void markProcessing(Instant leaseUntil) {
    if (status == OutboxStatus.PROCESSING) {
      retryCount++;
    }
    status = OutboxStatus.PROCESSING;
    this.leaseUntil = leaseUntil;
  }

  public void markPublished(Instant publishedAt) {
    status = OutboxStatus.PUBLISHED;
    leaseUntil = null;
    lastError = null;
    this.publishedAt = publishedAt;
  }

  public void markFailed(String error, int maxRetries) {
    retryCount++;
    status = retryCount > maxRetries ? OutboxStatus.FAILED : OutboxStatus.PENDING;
    leaseUntil = null;
    lastError = error;
  }

  public void markFailed(String error) {
    markFailed(error, 4);
  }

  public void exhaustLease(String error) {
    status = OutboxStatus.FAILED;
    leaseUntil = null;
    if (lastError == null) {
      lastError = error;
    }
  }
}
