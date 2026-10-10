package com.example.notification.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record MarkNotificationsReadBody(
        @NotEmpty(message = "notificationIds must not be empty")
                List<@NotNull UUID> notificationIds) {
}
