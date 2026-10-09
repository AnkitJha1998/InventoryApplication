package com.example.inventory.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory_operations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryOperation {
  @Id
  @Column(name = "operation_id", nullable = false)
  private UUID operationId;

  @Column(name = "order_id", nullable = false)
  private String orderId;

  @Column(name = "product_id", nullable = false)
  private String productId;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "previous_count", nullable = false)
  private int previousCount;

  @Column(name = "current_count", nullable = false)
  private int currentCount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
