package com.example.inventory.api;

public record CreateResponse(
        String productId,
        String productName,
        String productType,
        int availableQuantity,
        long version) {
}
