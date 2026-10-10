package com.example.notification.service;

import com.example.notification.api.BackInStockRequestBody;
import com.example.notification.consumer.NotificationTrigger;
import com.example.notification.persistence.entity.BackInStockRequest;
import com.example.notification.persistence.entity.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

public interface NotificationService {
  BackInStockRequest requestBackInStock(
      UUID customerId, BackInStockRequestBody body, String authorization)
      throws NotificationException;

  void process(NotificationTrigger trigger) throws NotificationException;

  long unreadCount(UUID customerId);

  Page<Notification> findNotifications(UUID customerId, int page, int size)
      throws NotificationException;

  int markAsRead(UUID customerId, List<UUID> notificationIds) throws NotificationException;
}
