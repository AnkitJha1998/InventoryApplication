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
        long version,
        String topic,
        Instant occurredAt) {
    public InventoryUpdate(
            UUID eventId,
            String productId,
            String productName,
            String productType,
            int previousCount,
            int currentCount,
            String topic,
            Instant occurredAt) {
        this(eventId, productId, productName, productType, previousCount, currentCount, 0, topic, occurredAt);
    }
}
