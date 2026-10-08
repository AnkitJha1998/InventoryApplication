package com.example.inventory.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_operations")
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

    protected InventoryOperation() {
    }

    public InventoryOperation(UUID operationId, String orderId, String productId, int quantity,
                              int previousCount, int currentCount) {
        this.operationId = operationId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.previousCount = previousCount;
        this.currentCount = currentCount;
        this.createdAt = Instant.now();
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPreviousCount() {
        return previousCount;
    }

    public int getCurrentCount() {
        return currentCount;
    }
}
