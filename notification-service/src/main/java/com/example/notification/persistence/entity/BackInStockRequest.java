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
@Table(name = "back_in_stock_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackInStockRequest {
  @Id
  @Column(name = "request_id", nullable = false)
  private UUID requestId;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "product_id", nullable = false)
  private String productId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private BackInStockStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
