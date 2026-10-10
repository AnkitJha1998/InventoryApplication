package com.example.notification.api;

import com.example.notification.persistence.entity.Notification;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;

public record NotificationPageResponse(
        List<NotificationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
    public static NotificationPageResponse from(Page<Notification> notifications) {
        return new NotificationPageResponse(
                notifications.getContent().stream()
                        .map(NotificationResponse::from)
                        .collect(Collectors.toList()),
                notifications.getNumber(),
                notifications.getSize(),
                notifications.getTotalElements(),
                notifications.getTotalPages());
    }
}
