package com.example.inventory.consumer;

import java.time.Instant;
import java.util.UUID;

public record InventoryUpdate(
        UUID eventId,
        String productId,
        String productName,
        String productType,
        int previousCount,
        int currentCount,
        String topic,
        Instant occurredAt) {
}
