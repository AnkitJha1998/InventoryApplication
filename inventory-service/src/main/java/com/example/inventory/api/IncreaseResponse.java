package com.example.inventory.api;

public record IncreaseResponse(
        String productId,
        int availableQuantity,
        long version) {
}
