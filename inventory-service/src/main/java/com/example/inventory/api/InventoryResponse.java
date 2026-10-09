package com.example.inventory.api;

import com.example.inventory.persistence.entity.InventoryDocument;
import java.time.Instant;

public record InventoryResponse(
        String productId,
        String productName,
        String productType,
        int availableCount,
        Instant updatedAt,
        int availableQuantity,
        boolean available,
        long version) {
    public InventoryResponse(
            String productId,
            String productName,
            String productType,
            int availableCount,
            Instant updatedAt) {
        this(productId, productName, productType, availableCount, updatedAt,
                availableCount, availableCount > 0, 0);
    }

    public InventoryResponse(
            String productId,
            String productName,
            String productType,
            int availableCount,
            Instant updatedAt,
            int availableQuantity,
            boolean available) {
        this(productId, productName, productType, availableCount, updatedAt,
                availableQuantity, available, 0);
    }

    public static InventoryResponse from(InventoryDocument document) {
        int quantity = document.getAvailableQuantity() == 0
                ? document.getAvailableCount()
                : document.getAvailableQuantity();
        return new InventoryResponse(
                document.getProductId(),
                document.getProductName(),
                document.getProductType(),
                document.getAvailableCount(),
                document.getUpdatedAt(),
                quantity,
                quantity > 0,
                document.getVersion());
    }
}
