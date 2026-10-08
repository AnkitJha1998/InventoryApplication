package com.example.inventory.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_type", nullable = false)
    private String productType;

    @Column(name = "available_count", nullable = false)
    private int availableCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Inventory() {
    }

    public Inventory(String productId, String productName, String productType, int availableCount) {
        this.productId = productId;
        this.productName = productName;
        this.productType = productType;
        this.availableCount = availableCount;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductType() {
        return productType;
    }

    public int getAvailableCount() {
        return availableCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
