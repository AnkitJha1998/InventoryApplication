package com.example.notification.consumer;

import java.time.Instant;
import java.util.UUID;

public record NotificationTrigger(
        UUID eventId,
        String productId,
        String productName,
        String productType,
        int previousCount,
        int currentCount,
        long version,
        Instant occurredAt,
        String topic) {
}
