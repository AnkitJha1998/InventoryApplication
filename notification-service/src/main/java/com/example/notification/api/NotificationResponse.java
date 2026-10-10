package com.example.notification.api;

import com.example.notification.persistence.entity.Notification;
import com.example.notification.persistence.entity.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID notificationId,
        String productId,
        String message,
        boolean read,
        Instant createdAt) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getProductId(),
                notification.getMessage(),
                notification.getStatus() == NotificationStatus.NOTIFIED,
                notification.getCreatedAt());
    }
}
